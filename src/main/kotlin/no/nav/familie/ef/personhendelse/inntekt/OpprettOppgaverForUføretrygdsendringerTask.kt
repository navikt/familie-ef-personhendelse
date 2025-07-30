package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettOppgaverForUføretrygdsendringerTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Oppretter oppgave for uføretrygsendringer på person",
)
class OpprettOppgaverForUføretrygdsendringerTask(
    private val inntektOppgaveService: InntektOppgaveService,
) : AsyncTaskStep {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        val personIdent = objectMapper.readValue<PayloadOpprettOppgaverForUføretrygdsendringerTask>(task.payload).personIdent
        secureLogger.info("Oppretter oppgaver for uføretrygdsendringer ${task.payload}")
        inntektOppgaveService.opprettOppgaveForUføretrygdEndring(personIdent, inntektOppgaveService.lagOppgavetekstVedEndringUføretrygd(YearMonth.now().minusMonths(1)))
    }

    companion object {
        const val TYPE = "OpprettOppgaverForUføretrygdsendringerTask"

        fun opprettTask(payload: String): Task {
            val payloadObject = objectMapper.readValue(payload, PayloadOpprettOppgaverForUføretrygdsendringerTask::class.java)

            return Task(
                type = TYPE,
                payload = payload,
                properties =
                    Properties().apply {
                        this["personIdent"] = payloadObject.personIdent
                        this["årMåned"] = payloadObject.årMåned
                    },
            )
        }
    }
}

data class PayloadOpprettOppgaverForUføretrygdsendringerTask(
    val personIdent: String,
    val årMåned: String,
)
