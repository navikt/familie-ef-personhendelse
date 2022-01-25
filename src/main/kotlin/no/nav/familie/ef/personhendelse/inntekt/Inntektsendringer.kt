package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.inntekt.vedtak.EfVedtakRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class Inntektsendringer(
    val vedtakRepository: EfVedtakRepository,
    val inntektClient: InntektClient,
    val oppgaveClient: OppgaveClient
) {

    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    fun beregnInntektsendringer() {
        val personerMedVedtakList = vedtakRepository.hentAllePersonerMedVedtak()

        secureLogger.info("Antall personer med aktive vedtak: ${personerMedVedtakList.size}")

        for (ensligForsørgerVedtakhendelse in personerMedVedtakList) {
            val response = inntektClient.hentInntektshistorikk(
                ensligForsørgerVedtakhendelse.personIdent,
                YearMonth.now().minusYears(1),
                null
            )
            if (harEndretInntekt(response)) {
                secureLogger.info("Person med behandlingId ${ensligForsørgerVedtakhendelse.personIdent} kan ha inntektsendringer. Skal opprette oppgave.")
            }
        }
    }

    fun harEndretInntekt(inntektshistorikkResponse: InntektshistorikkResponse): Boolean {
        // hent alle registrerte vedtak som var på personen sist beregning
        val nyesteRegistrerteInntekt = inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(1).toString())
        val nestNyesteRegistrerteInntekt = inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(2).toString())

        val sumOffentligeYtelserForNyeste = sumOffentligeYtelser(nyesteRegistrerteInntekt)
        val sumOffentligeYtelserForNestNyeste = sumOffentligeYtelser(nestNyesteRegistrerteInntekt)

        return sumOffentligeYtelserForNyeste > sumOffentligeYtelserForNestNyeste
    }

    private fun sumOffentligeYtelser(nyesteRegistrerteInntekt: List<InntektVersjon>?): Int {
        val nyesteVersjon = nyesteRegistrerteInntekt?.maxOf { it.versjon }

        val inntektListe = nyesteRegistrerteInntekt?.firstOrNull { it.versjon == nyesteVersjon }?.arbeidsInntektInformasjon?.inntektListe
        val beløpYtelseFraOffentligList = inntektListe?.filter { it.inntektType == InntektType.YTELSE_FRA_OFFENTLIGE }?.map { it.beløp } ?: listOf()
        return beløpYtelseFraOffentligList.sumOf { it }
    }
}
