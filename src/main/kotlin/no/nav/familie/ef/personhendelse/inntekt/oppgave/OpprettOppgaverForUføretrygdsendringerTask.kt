package no.nav.familie.ef.personhendelse.inntekt.oppgave

import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.readValue
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
        val personIdent = jsonMapper.readValue<PayloadOpprettOppgaverForUføretrygdsendringerTask>(task.payload).personIdent
        secureLogger.info("Oppretter oppgaver for uføretrygdsendringer ${task.payload}")
        inntektOppgaveService.opprettOppgaveForUføretrygdEndring(personIdent, inntektOppgaveService.lagOppgavetekstVedEndringUføretrygd(YearMonth.now().minusMonths(1)))
    }

    companion object {
        const val TYPE = "opprettOppgaverForUføretrygdsendringerTask"

        fun opprettTask(payload: PayloadOpprettOppgaverForUføretrygdsendringerTask): Task =
            Task(
                type = TYPE,
                payload = jsonMapper.writeValueAsString(payload),
                properties =
                    Properties().apply {
                        this["personIdent"] = payload.personIdent
                        this["årMåned"] = payload.årMåned.toString()
                    },
            )
    }
}

data class PayloadOpprettOppgaverForUføretrygdsendringerTask(
    val personIdent: String,
    val årMåned: YearMonth,
)
