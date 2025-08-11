package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.Properties
import kotlin.collections.set

@Service
@TaskStepBeskrivelse(
    taskStepType = FinnPersonerMedEndringUføretrygdTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Finner personer med endring i uføretrygd de siste to måneder og oppretter tasker for opprettelse av oppgaver.",
)
class FinnPersonerMedEndringUføretrygdTask(
    private val inntektsendringerService: InntektsendringerService,
    private val taskService: TaskService,
) : AsyncTaskStep {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        val (inntektsendringForBrukereMedUføretrygd, årMåned) = objectMapper.readValue<PayloadFinnPersonerMedEndringUføretrygdTask>(task.payload)
        val forrigeMåned = YearMonth.now().minusMonths(1)
        val toMånederTilbake = YearMonth.now().minusMonths(2)
        val kandidater =
            inntektsendringForBrukereMedUføretrygd.mapNotNull { personIdent ->
                val inntekt = inntektsendringerService.hentInntekt(personIdent) ?: return@mapNotNull null

                val uføretrygdForrige =
                    inntekt.inntektsmåneder
                        .filter { it.måned == forrigeMåned }
                        .flatMap { it.inntektListe }
                        .filter { it.beskrivelse == "ufoeretrygd" }
                        .sumOf { it.beløp }

                val uføretrygdToMnd =
                    inntekt.inntektsmåneder
                        .filter { it.måned == toMånederTilbake }
                        .flatMap { it.inntektListe }
                        .filter { it.beskrivelse == "ufoeretrygd" }
                        .sumOf { it.beløp }

                if (uføretrygdForrige > uføretrygdToMnd) personIdent else null
            }
        kandidater.forEach { kandidat ->
            val payload = PayloadOpprettOppgaverForUføretrygdsendringerTask(personIdent = kandidat, årMåned = årMåned)
            val skalOppretteTask = taskService.finnTaskMedPayloadOgType(objectMapper.writeValueAsString(payload), OpprettOppgaverForUføretrygdsendringerTask.TYPE) == null

            if (skalOppretteTask) {
                val task = OpprettOppgaverForUføretrygdsendringerTask.opprettTask(payload)
                taskService.save(task)
            }
        }
    }

    companion object {
        const val TYPE = "finnPersonerMedEndringUføretrygdTask"

        fun opprettTask(payload: PayloadFinnPersonerMedEndringUføretrygdTask): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(payload),
            )
    }
}

data class PayloadFinnPersonerMedEndringUføretrygdTask(
    val personIdenter: List<String>,
    val årMåned: YearMonth,
)
