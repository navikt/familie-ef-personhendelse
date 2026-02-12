package no.nav.familie.ef.personhendelse.inntekt.oppgave

import com.fasterxml.jackson.module.kotlin.readValue
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
        val personIdent = jsonMapper.readValue<PayloadOpprettOppgaverForArbeidsavklaringspengerEndringerTask>(task.payload).personIdent
        secureLogger.info("Oppretter oppgaver for arbeidsavklaringspenger endringer ${task.payload}")
        inntektOppgaveService.opprettOppgaveForArbeidsavklaringspengerEndring(personIdent, inntektOppgaveService.lagOppgavetekstVedEndringArbeidsavklaringspenger())
    }

    companion object {
        const val TYPE = "opprettOppgaverForArbeidsavklaringspengerEndringerTask"

        fun opprettTask(payload: PayloadOpprettOppgaverForArbeidsavklaringspengerEndringerTask): Task =
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

data class PayloadOpprettOppgaverForArbeidsavklaringspengerEndringerTask(
    val personIdent: String,
    val årMåned: YearMonth,
)
