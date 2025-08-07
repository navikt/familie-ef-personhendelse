package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = LoggInntektForPersonTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Logger inntekt for gitt person",
)
class LoggInntektForPersonTask(
    private val inntektClient: InntektClient,
) : AsyncTaskStep {
    private val securelogger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        val inntekt = inntektClient.hentInntekt(personIdent = task.payload, YearMonth.now().minusMonths(12), YearMonth.now())
        securelogger.info("InntektResponse for person ${task.payload}")
        securelogger.info("${objectMapper.writeValueAsString(inntekt)}")
    }

    companion object {
        const val TYPE = "loggInntektForPerson"

        fun opprettTask(payload: String): Task =
            Task(
                type = TYPE,
                payload = payload,
                properties =
                    Properties().apply {
                        this["personIdent"] = payload
                    },
            )
    }
}
