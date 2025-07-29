package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettOppgaverForArbeidsavklaringspengerEndringerTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Oppretter oppgave for arbeidsavklaringspenger endringer på person",
)
class OpprettOppgaverForArbeidsavklaringspengerEndringerTask(
    private val inntektOppgaveService: InntektOppgaveService,
) : AsyncTaskStep {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        val personIdent = objectMapper.readValue<PayloadOpprettOppgaverForArbeidsavklaringspengerEndringerTask>(task.payload).personIdent
        logger.info("Oppretter oppgaver for arbeidsavklaringspenger endringer ${task.payload}")
        inntektOppgaveService.opprettOppgaveForArbeidsavklaringspengerEndring(personIdent, inntektOppgaveService.lagOppgavetekstVedEndringArbeidsavklaringspenger())
    }

    companion object {
        const val TYPE = "OpprettOppgaverForArbeidsavklaringspengerEndringerTask"

        fun opprettTask(payload: String): Task {
            val payloadObject = objectMapper.readValue(payload, PayloadOpprettOppgaverForArbeidsavklaringspengerEndringerTask::class.java)

            return Task(
                type = TYPE,
                payload = payload,
                properties =
                    Properties().apply {
                        this["personIdent"] = payloadObject.personIdent
                        this["måned"] = payloadObject.måned
                    },
            )
        }
    }
}

data class PayloadOpprettOppgaverForArbeidsavklaringspengerEndringerTask(
    val personIdent: String,
    val måned: String,
)
