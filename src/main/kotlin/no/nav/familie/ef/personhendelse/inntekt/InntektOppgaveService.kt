package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.client.ArbeidsfordelingClient
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.fristFerdigstillelse
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Service
class InntektOppgaveService(
    val oppgaveClient: OppgaveClient,
    val sakClient: SakClient,
    val arbeidsfordelingClient: ArbeidsfordelingClient,
    val inntektsendringerRepository: InntektsendringerRepository,
    val inntektsendringerService: InntektsendringerService,
) {
    @Async
    fun opprettOppgaverForUføretrygdsendringerAsync(skalOppretteOppgave: Boolean) {
        opprettOppgaveForUføretrygdsendringer(skalOppretteOppgave)
    }

    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    fun opprettOppgaverForInntektsendringer(skalOppretteOppgave: Boolean): Int {
        val inntektsendringer = inntektsendringerRepository.hentInntektsendringerSomSkalHaOppgave()
        if (skalOppretteOppgave) {
            inntektsendringer.forEach {
                opprettOppgaveForInntektsendring(it, lagOppgavetekstForInntektsendring(it))
            }
        } else {
            logger.info("Ville opprettet inntektsendring-oppgave for ${inntektsendringer.size} personer")
        }
        return inntektsendringer.size
    }

    fun opprettOppgaveForUføretrygdsendringer(
        skalOppretteOppgave: Boolean,
    ): Int {
        val inntektsendringForBrukereMedUføretrygd = inntektsendringerRepository.hentInntektsendringerForUføretrygdSomHarUføretrygd()
        val forrigeMåned = YearMonth.now().minusMonths(1)
        val toMånederTilbake = YearMonth.now().minusMonths(2)
        val kandidater =
            inntektsendringForBrukereMedUføretrygd.mapNotNull { endring ->
                val inntekt = inntektsendringerService.hentInntekt(endring.personIdent) ?: return@mapNotNull null

                val uføretrygdForrige =
                    inntekt.inntektsmåneder
                        .find { it.måned == forrigeMåned }
                        ?.inntektListe
                        ?.filter { it.beskrivelse == "ufoeretrygd" }
                        ?.sumOf { it.beløp }
                        ?: 0.0

                val uføretrygdToMnd =
                    inntekt.inntektsmåneder
                        .find { it.måned == toMånederTilbake }
                        ?.inntektListe
                        ?.filter { it.beskrivelse == "ufoeretrygd" }
                        ?.sumOf { it.beløp }
                        ?: 0.0

                if (uføretrygdForrige > uføretrygdToMnd) endring else null
            }

        if (skalOppretteOppgave) {
            kandidater.forEach {
                opprettOppgaveForUføretrygdEndring(it, lagOppgavetekstVedEndringUføretrygd(forrigeMåned))
            }
        } else {
            logger.info("Ville opprettet uføretrygdsendring-oppgave for ${kandidater.size} personer")
        }
        return kandidater.size
    }

    fun opprettOppgaverForNyeVedtakUføretrygd() {
        val nyeUføretrygdVedtak = inntektsendringerRepository.hentInntektsendringerForUføretrygd()
        nyeUføretrygdVedtak.forEach {
            opprettOppgaveForInntektsendring(it, lagOppgavetekstVedNyYtelseUføretrygd())
        }
    }

    private fun opprettOppgaveForInntektsendring(
        inntektOgVedtakEndring: InntektOgVedtakEndring,
        beskrivelse: String,
    ) {
        val oppgaveId =
            oppgaveClient.opprettOppgave(
                OpprettOppgaveRequest(
                    ident =
                        OppgaveIdentV2(
                            ident = inntektOgVedtakEndring.personIdent,
                            gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                        ),
                    saksId = null,
                    tema = Tema.ENF,
                    oppgavetype = Oppgavetype.VurderInntekt,
                    fristFerdigstillelse = fristFerdigstillelse(),
                    beskrivelse = beskrivelse,
                    enhetsnummer = arbeidsfordelingClient.hentArbeidsfordelingEnhetId(inntektOgVedtakEndring.personIdent),
                    behandlingstema = null, // Gjelder-feltet i Gosys
                    tilordnetRessurs = null,
                    behandlesAvApplikasjon = null,
                ),
            )
        secureLogger.info("Opprettet oppgave for person ${inntektOgVedtakEndring.personIdent} med id: $oppgaveId")
        oppgaveClient.leggOppgaveIMappe(oppgaveId, "63") // Inntektskontroll
    }

    private fun opprettOppgaveForUføretrygdEndring(
        inntektOgVedtakEndring: InntektOgVedtakEndring,
        beskrivelse: String,
    ) {
        val oppgaveId =
            oppgaveClient.opprettOppgave(
                OpprettOppgaveRequest(
                    ident =
                        OppgaveIdentV2(
                            ident = inntektOgVedtakEndring.personIdent,
                            gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                        ),
                    saksId = null,
                    tema = Tema.ENF,
                    oppgavetype = Oppgavetype.VurderKonsekvensForYtelse,
                    fristFerdigstillelse = fristFerdigstillelse(),
                    beskrivelse = beskrivelse,
                    enhetsnummer = "4489",
                    behandlingstema = Behandlingstema.Overgangsstønad.value, // Gjelder-feltet i Gosys
                    tilordnetRessurs = null,
                    behandlesAvApplikasjon = null,
                ),
            )
        secureLogger.info("Opprettet oppgave for person ${inntektOgVedtakEndring.personIdent} med id: $oppgaveId")
        oppgaveClient.leggOppgaveIMappe(oppgaveId, "63") // Inntektskontroll
    }

    fun lagOppgavetekstVedNyYtelseUføretrygd(): String = "Bruker har fått utbetalt uføretrygd. Vurder samordning."

    fun lagOppgavetekstVedEndringUføretrygd(MånedÅr: YearMonth): String = "Uføretrygden til bruker har økt fra ${MånedÅr.norskFormat()}. Vurder om overgangsstønaden skal beregnes på nytt."

    fun lagOppgavetekstForInntektsendring(inntektOgVedtakEndring: InntektOgVedtakEndring): String {
        val totalFeilutbetaling =
            inntektOgVedtakEndring.inntektsendringTreMånederTilbake.feilutbetaling +
                inntektOgVedtakEndring.inntektsendringToMånederTilbake.feilutbetaling +
                inntektOgVedtakEndring.inntektsendringForrigeMåned.feilutbetaling

        val årMånedProsessert = YearMonth.from(inntektOgVedtakEndring.prosessertTid)

        val periodeTekst =
            "FOM ${årMånedProsessert.minusMonths(3).norskFormat()} - TOM ${årMånedProsessert.minusMonths(1).norskFormat()}"
        val oppgavetekst =
            "Uttrekksperiode: $periodeTekst \n" +
                "Beregnet feilutbetaling i uttrekksperioden: ${totalFeilutbetaling.tusenskille()} kroner "
        return oppgavetekst
    }

    private fun YearMonth.norskFormat() = this.format(DateTimeFormatter.ofPattern("MM.yyyy"))

    private fun Int.tusenskille() = String.format("%,d", this).replace(",", " ")
}
