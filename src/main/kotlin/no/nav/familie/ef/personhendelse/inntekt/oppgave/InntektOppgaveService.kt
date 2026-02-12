package no.nav.familie.ef.personhendelse.inntekt.oppgave

import no.nav.familie.ef.personhendelse.client.ArbeidsfordelingClient
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.fristFerdigstillelse
import no.nav.familie.ef.personhendelse.inntekt.InntektsendringerRepository
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
class InntektOppgaveService(
    val oppgaveClient: OppgaveClient,
    val sakClient: SakClient,
    val arbeidsfordelingClient: ArbeidsfordelingClient,
    val inntektsendringerRepository: InntektsendringerRepository,
    val taskService: TaskService,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    fun opprettOppgaverForInntektsendringer(): Int {
        val inntektsendringer = inntektsendringerRepository.hentInntektsendringerSomSkalHaOppgave()
        inntektsendringer.forEach {
            val totalFeilutbetaling =
                it.inntektsendringTreMånederTilbake.feilutbetaling +
                    it.inntektsendringToMånederTilbake.feilutbetaling +
                    it.inntektsendringForrigeMåned.feilutbetaling
            val yearMonthProssesertTid = YearMonth.from(it.prosessertTid)
            val payload = PayloadOpprettOppgaverForInntektsendringerTask(personIdent = it.personIdent, totalFeilutbetaling = totalFeilutbetaling, yearMonthProssesertTid = yearMonthProssesertTid)
            val skalOppretteTask = taskService.finnTaskMedPayloadOgType(jsonMapper.writeValueAsString(payload), OpprettOppgaverForInntektsendringerTask.TYPE) == null

            if (skalOppretteTask) {
                val task = OpprettOppgaverForInntektsendringerTask.opprettTask(payload)
                taskService.save(task)
            }
        }
        return inntektsendringer.size
    }

    fun finnPersonerMedEndringUføretrygdToSisteMånederOgOpprettOppgaver() {
        val personIdenter = inntektsendringerRepository.hentInntektsendringerForPersonerMedUføretrygd()
        val payload = PayloadFinnPersonerMedEndringUføretrygdTask(personIdenter = personIdenter, årMåned = YearMonth.now())
        val skalOppretteTask = taskService.finnTaskMedPayloadOgType(jsonMapper.writeValueAsString(payload), FinnPersonerMedEndringUføretrygdTask.TYPE) == null

        if (skalOppretteTask) {
            val task = FinnPersonerMedEndringUføretrygdTask.opprettTask(payload)
            taskService.save(task)
        }
    }

    fun finnPersonerSomHarFyltTjueFemOgHarArbeidsavklaringspengerOgOpprettOppgaver() {
        val personIdenterBrukerereMedArbeidsavklaringspenger = inntektsendringerRepository.hentInntektsendringerForPersonMedArbeidsavklaringspenger()
        val payload = PayloadFinnPersonerFyltTjueFemArbeidsavklaringspengerTask(personIdenterBrukereMedArbeidsavklaringspenger = personIdenterBrukerereMedArbeidsavklaringspenger, årMåned = YearMonth.now())
        val task = FinnPersonerFyltTjueFemArbeidsavklaringspengerTask.opprettTask(payload)
        taskService.save(task)
    }

    fun opprettOppgaverForNyeVedtakUføretrygd() {
        val nyeUføretrygdVedtak = inntektsendringerRepository.hentInntektsendringerForUføretrygd()
        nyeUføretrygdVedtak.forEach {
            val yearMonthProssesertTid = YearMonth.from(it.prosessertTid)
            val payload = PayloadOpprettOppgaverForNyeVedtakUføretrygdTask(personIdent = it.personIdent, prosessertYearMonth = yearMonthProssesertTid)
            val skalOppretteTask = taskService.finnTaskMedPayloadOgType(jsonMapper.writeValueAsString(payload), OpprettOppgaverForNyeVedtakUføretrygdTask.TYPE) == null

            if (skalOppretteTask) {
                val task = OpprettOppgaverForNyeVedtakUføretrygdTask.opprettTask(payload)
                taskService.save(task)
            }
        }
    }

    fun opprettOppgaveForInntektsendring(
        personIdent: String,
        beskrivelse: String,
    ) {
        val oppgaveId =
            oppgaveClient.opprettOppgave(
                OpprettOppgaveRequest(
                    ident =
                        OppgaveIdentV2(
                            ident = personIdent,
                            gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                        ),
                    saksId = null,
                    tema = Tema.ENF,
                    oppgavetype = Oppgavetype.VurderInntekt,
                    fristFerdigstillelse = fristFerdigstillelse(),
                    beskrivelse = beskrivelse,
                    enhetsnummer = arbeidsfordelingClient.hentArbeidsfordelingEnhetId(personIdent),
                    behandlingstema = null, // Gjelder-feltet i Gosys
                    tilordnetRessurs = null,
                    behandlesAvApplikasjon = null,
                ),
            )
        secureLogger.info("Opprettet inntektsendring oppgave for person $personIdent med id: $oppgaveId")
        oppgaveClient.leggOppgaveIMappe(oppgaveId, "63") // Inntektskontroll
    }

    fun opprettOppgaveForUføretrygdEndring(
        personIdent: String,
        beskrivelse: String,
    ) {
        val oppgaveId =
            oppgaveClient.opprettOppgave(
                OpprettOppgaveRequest(
                    ident =
                        OppgaveIdentV2(
                            ident = personIdent,
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
        secureLogger.info("Opprettet uføretrygdsendring oppgave for person $personIdent med id: $oppgaveId")
        oppgaveClient.leggOppgaveIMappe(oppgaveId, "63") // Inntektskontroll
    }

    fun opprettOppgaveForArbeidsavklaringspengerEndring(
        personIdent: String,
        beskrivelse: String,
    ) {
        val oppgaveId =
            oppgaveClient.opprettOppgave(
                OpprettOppgaveRequest(
                    ident =
                        OppgaveIdentV2(
                            ident = personIdent,
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
        secureLogger.info("Opprettet arbeidsavklaeringspenger endring oppgave for person $personIdent med id: $oppgaveId")
        oppgaveClient.leggOppgaveIMappe(oppgaveId, "63") // Inntektskontroll
    }

    fun lagOppgavetekstVedNyYtelseUføretrygd(): String = "Bruker har fått utbetalt uføretrygd. Vurder samordning."

    fun lagOppgavetekstVedEndringUføretrygd(MånedÅr: YearMonth): String = "Uføretrygden til bruker har økt fra ${MånedÅr.norskFormat()}. Vurder om overgangsstønaden skal beregnes på nytt."

    fun lagOppgavetekstVedEndringArbeidsavklaringspenger(): String = "Bruker mottar AAP og har fylt 25 år. Kontroller inntekt på grunn av økt dagsats."

    fun lagOppgavetekstForInntektsendring(
        totalFeilutbetaling: Int,
        yearMonthProssertTid: YearMonth,
    ): String {
        val periodeTekst =
            "FOM ${yearMonthProssertTid.minusMonths(3).norskFormat()} - TOM ${yearMonthProssertTid.minusMonths(1).norskFormat()}"
        val oppgavetekst =
            "Uttrekksperiode: $periodeTekst \n" +
                "Beregnet feilutbetaling i uttrekksperioden: ${totalFeilutbetaling.tusenskille()} kroner "
        return oppgavetekst
    }

    private fun YearMonth.norskFormat() = this.format(DateTimeFormatter.ofPattern("MM.yyyy"))

    private fun Int.tusenskille(): String {
        val symbols =
            DecimalFormatSymbols(Locale.US).apply {
                groupingSeparator = ' ' // U+0020
            }
        val formatter = DecimalFormat("#,###", symbols)
        return formatter.format(this)
    }
}
