package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.inntekt.vedtak.EfVedtakRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class InntektsendringerService(
    val vedtakRepository: EfVedtakRepository,
    val inntektClient: InntektClient,
    val oppgaveClient: OppgaveClient,
    val sakClient: SakClient
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun beregnInntektsendringer() {
        val personerMedVedtakList = vedtakRepository.hentAllePersonerMedVedtak()

        logger.info("Antall personer med aktive vedtak: ${personerMedVedtakList.size}")

        for (ensligForsørgerVedtakhendelse in personerMedVedtakList) {
            if (sakClient.harAktivtVedtak(ensligForsørgerVedtakhendelse.behandlingId)) {
                val response = inntektClient.hentInntektshistorikk(
                    ensligForsørgerVedtakhendelse.personIdent,
                    YearMonth.now().minusYears(1),
                    null
                )
                if (harEndretInntekt(response, ensligForsørgerVedtakhendelse.behandlingId)) {
                    logger.info("Person med behandlingId ${ensligForsørgerVedtakhendelse.behandlingId} kan ha inntektsendringer. Skal opprette oppgave.")
                }
            }
        }
    }

    fun harEndretInntekt(inntektshistorikkResponse: InntektshistorikkResponse, eksternId: Long): Boolean {
        // hent alle registrerte vedtak som var på personen sist beregning
        val nyesteRegistrerteInntekt = inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(1).toString())
        val nestNyesteRegistrerteInntekt = inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(2).toString())

        return harMottattMerIOffentligeYtelser(nestNyesteRegistrerteInntekt, nyesteRegistrerteInntekt) || har10ProsentHøyereInntektEnnForventet(nyesteRegistrerteInntekt, eksternId)
    }

    private fun har10ProsentHøyereInntektEnnForventet(nyesteRegistrerteInntekt: List<InntektVersjon>?, eksternId: Long): Boolean {
        val forventetInntekt = sakClient.inntektForEksternId(eksternId)
        if (forventetInntekt == null) {
            logger.warn("Ingen gjeldende inntekt funnet på person med ekstern behandlingid $eksternId - har personen løpende stønad?")
            return false
        }
        val månedligForventetInntekt = (forventetInntekt / 12)

        val nyesteVersjon = nyesteRegistrerteInntekt?.maxOf { it.versjon }
        val inntektListe = nyesteRegistrerteInntekt?.firstOrNull { it.versjon == nyesteVersjon }?.arbeidsInntektInformasjon?.inntektListe

        val samletInntekt = inntektListe?.filter { it.inntektType != InntektType.YTELSE_FRA_OFFENTLIGE }?.sumOf { it.beløp } ?: 0
        logger.info("Samlet inntekt: $samletInntekt - månedlig forventet inntekt: $månedligForventetInntekt  (årlig: $forventetInntekt) for person med behandlingId (eksternId) $eksternId")
        return samletInntekt >= (månedligForventetInntekt * 1.1)
    }

    private fun harMottattMerIOffentligeYtelser(nestNyesteRegistrerteInntekt: List<InntektVersjon>?, nyesteRegistrerteInntekt: List<InntektVersjon>?): Boolean {
        val sumOffentligeYtelserForNyeste = sumOffentligeYtelser(nyesteRegistrerteInntekt)
        val sumOffentligeYtelserForNestNyeste = sumOffentligeYtelser(nestNyesteRegistrerteInntekt)

        return (sumOffentligeYtelserForNyeste > sumOffentligeYtelserForNestNyeste)
    }

    private fun sumOffentligeYtelser(nyesteRegistrerteInntekt: List<InntektVersjon>?): Int {
        val nyesteVersjon = nyesteRegistrerteInntekt?.maxOf { it.versjon }

        val inntektListe = nyesteRegistrerteInntekt?.firstOrNull { it.versjon == nyesteVersjon }?.arbeidsInntektInformasjon?.inntektListe
        val beløpYtelseFraOffentligList = inntektListe?.filter { it.inntektType == InntektType.YTELSE_FRA_OFFENTLIGE && it.inntektsperiodetype == "Maaned" }?.map { it.beløp } ?: listOf() // Sjekker kun mot faste månedlige utbetalinger (ikke dagpenger / AAP fordi de ofte er variable)
        return beløpYtelseFraOffentligList.sumOf { it }
    }
}
