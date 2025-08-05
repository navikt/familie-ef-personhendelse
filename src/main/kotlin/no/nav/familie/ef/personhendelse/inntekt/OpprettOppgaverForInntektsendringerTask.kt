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
    taskStepType = OpprettOppgaverForInntektsendringerTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Oppretter oppgave for arbeidsavklaringspenger endringer på person",
)
class OpprettOppgaverForInntektsendringerTask(
    private val inntektOppgaveService: InntektOppgaveService,
) : AsyncTaskStep {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        val (personIdent, totalFeilutbetaling, yearMonthProssesertTid) = objectMapper.readValue<PayloadOpprettOppgaverForInntektsendringerTask>(task.payload)
        secureLogger.info("Oppretter oppgaver for inntektsendringer ${task.payload}")
        inntektOppgaveService.opprettOppgaveForInntektsendring(personIdent, inntektOppgaveService.lagOppgavetekstForInntektsendring(totalFeilutbetaling, yearMonthProssesertTid))
    }

    companion object {
        const val TYPE = "OpprettOppgaverForInntektsendringerTask"

        fun opprettTask(payload: String): Task {
            val payloadObject = objectMapper.readValue(payload, PayloadOpprettOppgaverForInntektsendringerTask::class.java)

            return Task(
                type = TYPE,
                payload = payload,
                properties =
                    Properties().apply {
                        this["personIdent"] = payloadObject.personIdent
                    },
            )
        }
    }
}

data class PayloadOpprettOppgaverForInntektsendringerTask(
    val personIdent: String,
    val totalFeilutbetaling: Int,
    val yearMonthProssesertTid: YearMonth,
)
