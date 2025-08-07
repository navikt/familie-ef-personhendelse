package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.client.ArbeidsfordelingClient
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.fristFerdigstillelse
import no.nav.familie.ef.personhendelse.client.pdl.PdlClient
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.objectMapper
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
class InntektOppgaveService(
    val oppgaveClient: OppgaveClient,
    val sakClient: SakClient,
    val arbeidsfordelingClient: ArbeidsfordelingClient,
    val inntektsendringerRepository: InntektsendringerRepository,
    val pdlClient: PdlClient,
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
            val payload = objectMapper.writeValueAsString(PayloadOpprettOppgaverForInntektsendringerTask(personIdent = it.personIdent, totalFeilutbetaling = totalFeilutbetaling, yearMonthProssesertTid = yearMonthProssesertTid))
            val skalOppretteTask = taskService.finnTaskMedPayloadOgType(payload, OpprettOppgaverForInntektsendringerTask.TYPE) == null

            if (skalOppretteTask) {
                val task = OpprettOppgaverForInntektsendringerTask.opprettTask(payload)
                taskService.save(task)
            }
        }
        return inntektsendringer.size
    }

    fun finnPersonerMedEndringUføretrygdToSisteMånederOgOpprettOppgaver() {
        val inntektsendringForBrukereMedUføretrygd = inntektsendringerRepository.hentInntektsendringerForPersonerMedUføretrygd()
        val payload = objectMapper.writeValueAsString(PayloadFinnPersonerMedEndringUføretrygdTask(inntektsendringForBrukereMedUføretrygd = inntektsendringForBrukereMedUføretrygd, årMåned = YearMonth.now()))
        val skalOppretteTask = taskService.finnTaskMedPayloadOgType(objectMapper.writeValueAsString(payload), FinnPersonerMedEndringUføretrygdTask.TYPE) == null

        if (skalOppretteTask) {
            val task = FinnPersonerMedEndringUføretrygdTask.opprettTask(payload)
            taskService.save(task)
        }
    }

    fun finnPersonerSomHarFyltTjueFemOgHarArbeidsavklaringspengerOgOpprettOppgaver() {
        val inntektsendringForBrukereMedArbeidsavklaringspenger = inntektsendringerRepository.hentInntektsendringerForPersonMedArbeidsavklaringspenger()
        val startDato = YearMonth.now().minusMonths(1).atDay(6)
        val sluttDato = LocalDate.now()

        val kandidater =
            inntektsendringForBrukereMedArbeidsavklaringspenger.mapNotNull { endring ->
                val person = pdlClient.hentPerson(endring.personIdent)
                val foedselsdato = person.foedselsdato.first().foedselsdato
                if (foedselsdato == null) {
                    secureLogger.error("Fant ingen fødselsdato for person ${endring.personIdent}")
                }
                val fylte25Dato = foedselsdato?.plusYears(25)
                if (fylte25Dato?.isAfter(startDato) == true && fylte25Dato.isBefore(sluttDato.plusDays(1))) endring else null
            }
        kandidater.forEach { kandidat ->
            val payload = PayloadOpprettOppgaverForArbeidsavklaringspengerEndringerTask(personIdent = kandidat.personIdent, årMåned = YearMonth.from(kandidat.prosessertTid))
            val skalOppretteTask = taskService.finnTaskMedPayloadOgType(objectMapper.writeValueAsString(payload), OpprettOppgaverForArbeidsavklaringspengerEndringerTask.TYPE) == null

            if (skalOppretteTask) {
                val task = OpprettOppgaverForArbeidsavklaringspengerEndringerTask.opprettTask(payload)
                taskService.save(task)
            }
        }
    }

    fun opprettOppgaverForNyeVedtakUføretrygd() {
        val nyeUføretrygdVedtak = inntektsendringerRepository.hentInntektsendringerForUføretrygd()
        nyeUføretrygdVedtak.forEach {
            val yearMonthProssesertTid = YearMonth.from(it.prosessertTid)
            val payload = objectMapper.writeValueAsString(PayloadOpprettOppgaverForNyeVedtakUføretrygdTask(personIdent = it.personIdent, prosessertYearMonth = yearMonthProssesertTid))
            val skalOppretteTask = taskService.finnTaskMedPayloadOgType(payload, OpprettOppgaverForNyeVedtakUføretrygdTask.TYPE) == null

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
