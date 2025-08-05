package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.Properties
import kotlin.collections.set

class OpprettOppgaverForNyeVedtakUføretrygdTask(
    private val inntektOppgaveService: InntektOppgaveService,
) : AsyncTaskStep {
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        val personIdent = objectMapper.readValue<PayloadOpprettOppgaverForNyeVedtakUføretrygdTask>(task.payload).personIdent
        secureLogger.info("Oppretter oppgaver for nye vedtak uføretrygd for person: $personIdent")
        inntektOppgaveService.opprettOppgaveForInntektsendring(personIdent, inntektOppgaveService.lagOppgavetekstVedNyYtelseUføretrygd())
    }

    companion object {
        const val TYPE = "OpprettOppgaverForNyeVedtakUføretrygdTask"

        fun opprettTask(payload: String): Task {
            val payloadObject = objectMapper.readValue(payload, PayloadOpprettOppgaverForNyeVedtakUføretrygdTask::class.java)

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

data class PayloadOpprettOppgaverForNyeVedtakUføretrygdTask(
    val personIdent: String,
    val yearMonthProssesertTid: YearMonth,
)
