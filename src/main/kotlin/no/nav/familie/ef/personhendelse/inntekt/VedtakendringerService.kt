package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.inntekt.vedtak.EfVedtakRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class VedtakendringerService(
    val efVedtakRepository: EfVedtakRepository,
    val inntektClient: InntektClient,
    val oppgaveClient: OppgaveClient,
    val sakClient: SakClient,
    val inntektsendringerService: InntektsendringerService
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    @Async
    fun beregnNyeVedtakOgLagOppgave() {
        // val personerMedVedtakList = efVedtakRepository.hentAllePersonerMedVedtak() // for testing

        val identToForventetInntektMap = sakClient.hentAlleAktiveIdenterOgForventetInntekt()
        logger.info("Antall personer med aktive vedtak: ${identToForventetInntektMap.keys.size}")

        // Kommer til å bytte til batch-prosessering for forbedring av ytelse
        for (identMedForventetInntekt in identToForventetInntektMap.entries) {

            val response = hentInntektshistorikk(identMedForventetInntekt)

            if (response != null && harNyeVedtak(response)) {
                secureLogger.info("Person ${identMedForventetInntekt.key} kan ha nye vedtak. Oppretter oppgave.")
                /*
                val oppgaveId = oppgaveClient.opprettOppgave(
                    defaultOpprettOppgaveRequest(
                        ensligForsørgerVedtakhendelse.personIdent,
                        "Sjekk om bruker har fått nytt vedtak"
                    )
                )
                secureLogger.info("Oppgave opprettet med id: $oppgaveId")
                 */
            }
            if (response != null && inntektsendringerService.harEndretInntekt(response, identMedForventetInntekt)) {
                secureLogger.info("Person ${identMedForventetInntekt.key} kan ha endret inntekt. Oppretter oppgave.")
            }
            // efVedtakRepository.oppdaterAarMaanedProsessert(identMedForventetInntekt.key)
        }
    }

    private fun hentInntektshistorikk(identMedForventetInntekt: Map.Entry<String, Int?>): InntektshistorikkResponse? {
        try {
            return inntektClient.hentInntektshistorikk(
                identMedForventetInntekt.key,
                YearMonth.now().minusYears(1),
                null
            )
        } catch (e: Exception) {
            secureLogger.warn("Feil ved kall mot inntektskomponenten ved kall mot person ${identMedForventetInntekt.key}. Message: ${e.message} Cause: ${e.cause}")
        }
        return null
    }

    fun harNyeVedtak(inntektshistorikkResponse: InntektshistorikkResponse): Boolean {
        // hent alle registrerte vedtak som var på personen sist beregning
        val nyesteRegistrerteInntekt =
            inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(1).toString())
        val nestNyesteRegistrerteInntekt =
            inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(2).toString())

        val antallOffentligeYtelserForNyeste = antallOffentligeYtelser(nyesteRegistrerteInntekt)
        val antallOffentligeYtelserForNestNyeste = antallOffentligeYtelser(nestNyesteRegistrerteInntekt)

        return antallOffentligeYtelserForNyeste > antallOffentligeYtelserForNestNyeste
    }

    private fun antallOffentligeYtelser(nyesteRegistrerteInntekt: List<InntektVersjon>?): Int {
        val nyesteVersjon = nyesteRegistrerteInntekt?.maxOf { it.versjon }

        val inntektListe =
            nyesteRegistrerteInntekt?.firstOrNull { it.versjon == nyesteVersjon }?.arbeidsInntektInformasjon?.inntektListe
        return inntektListe?.filter {
            it.inntektType == InntektType.YTELSE_FRA_OFFENTLIGE &&
                it.beskrivelse != "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere"
        }?.groupBy { it.beskrivelse }?.size ?: 0
    }
}
