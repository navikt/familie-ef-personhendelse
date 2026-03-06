package no.nav.familie.ef.personhendelse.inntekt.oppgave

import no.nav.familie.ef.personhendelse.client.pdl.PdlClient
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.readValue
import java.time.YearMonth

@Service
@TaskStepBeskrivelse(
    taskStepType = FinnPersonerFyltTjueFemArbeidsavklaringspengerTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Sjekker om person som mottar arbeidsavklaringspenger har fylt 25 den siste måneden.",
)
class FinnPersonerFyltTjueFemArbeidsavklaringspengerTask(
    private val pdlClient: PdlClient,
    private val taskService: TaskService,
) : AsyncTaskStep {
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        val (personIdent, årMåned) = jsonMapper.readValue<PayloadFinnPersonerFyltTjueFemArbeidsavklaringspengerTask>(task.payload)
        val startDato = årMåned.minusMonths(1).atDay(6)
        val sluttDato = årMåned.atDay(7)

        val person = pdlClient.hentPerson(personIdent)
        val foedselsdato = person.foedselsdato.first().foedselsdato
        if (foedselsdato == null) {
            secureLogger.error("Fant ingen fødselsdato for person $personIdent")
            return
        }

        val fylte25Dato = foedselsdato.plusYears(25)
        if (fylte25Dato.isAfter(startDato) && fylte25Dato.isBefore(sluttDato)) {
            val payload = PayloadOpprettOppgaverForArbeidsavklaringspengerEndringerTask(personIdent = personIdent, årMåned = årMåned)
            val skalOppretteTask =
                taskService.finnTaskMedPayloadOgType(
                    jsonMapper.writeValueAsString(payload),
                    OpprettOppgaverForArbeidsavklaringspengerEndringerTask.TYPE,
                ) == null

            if (skalOppretteTask) {
                val oppgaveTask = OpprettOppgaverForArbeidsavklaringspengerEndringerTask.opprettTask(payload)
                taskService.save(oppgaveTask)
            }
        }
    }

    companion object {
        const val TYPE = "finnPersonerFyltTjueFemArbeidsavklaringspengerTask"

        fun opprettTask(payload: PayloadFinnPersonerFyltTjueFemArbeidsavklaringspengerTask): Task =
            Task(
                type = TYPE,
                payload = jsonMapper.writeValueAsString(payload),
            )
    }
}

data class PayloadFinnPersonerFyltTjueFemArbeidsavklaringspengerTask(
    val personIdent: String,
    val årMåned: YearMonth,
)
