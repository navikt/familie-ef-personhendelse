package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.client.pdl.secureLogger
import no.nav.familie.ef.personhendelse.handler.PersonhendelseService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import java.time.YearMonth
import java.util.Properties

@TaskStepBeskrivelse(
    taskStepType = LoggInntektForPersonTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Oppretter oppgave for uføretrygsendringer på person",
)
class LoggInntektForPersonTask(
    private val inntektClient: InntektClient,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val inntekt = inntektClient.hentInntekt(personIdent = task.payload, YearMonth.now().minusMonths(12), YearMonth.now())
        secureLogger.info("InntektResponse for person ${task.payload}")
        secureLogger.info("${objectMapper.writeValueAsString(inntekt)}")
    }

    companion object {
        const val TYPE = "logg-inntekt-for-person"

        fun opprettTask(payload: String): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(payload),
                properties =
                    Properties().apply {
                        this["personIdent"] = payload
                    },
            )

    }
}