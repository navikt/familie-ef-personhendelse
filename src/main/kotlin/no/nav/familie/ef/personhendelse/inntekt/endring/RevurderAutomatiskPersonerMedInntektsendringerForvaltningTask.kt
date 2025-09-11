package no.nav.familie.ef.personhendelse.inntekt.endring

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.inntekt.InntektClient
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.Properties
import kotlin.collections.set
import kotlin.math.abs

@Service
@TaskStepBeskrivelse(
    taskStepType = RevurderAutomatiskPersonerMedInntektsendringerForvaltningTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Revurder automatisk person med inntektsendringer",
)
class RevurderAutomatiskPersonerMedInntektsendringerForvaltningTask(
    private val sakClient: SakClient,
    private val inntektClient: InntektClient,
    private val inntektsendringService: InntektsendringerService,
) : AsyncTaskStep {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        val (personIdent, harIngenEksisterendeYtelser, yearMonthProssesertTid) = objectMapper.readValue<PayloadRevurderAutomatiskPersonerMedInntektsendringerTask>(task.payload)
        secureLogger.info("Reverdurer automatisk person med inntektsendringer: ${task.payload}")
        val inntektResponse = inntektClient.hentInntekt(personIdent, yearMonthProssesertTid.minusMonths(3), yearMonthProssesertTid.minusMonths(1))
        val harStabilInntekt = inntektsendringService.harStabilInntektOgLoggInntekt(inntektResponse, yearMonthProssesertTid, personIdent, harIngenEksisterendeYtelser)
        if (harStabilInntekt && harIngenEksisterendeYtelser) {
            sakClient.revurderAutomatiskForvaltning(listOf<String>(personIdent))
        }
    }

    companion object {
        const val TYPE = "revurderAutomatiskPersonerMedInntektsendringerForvaltningTask"

        fun opprettTask(payload: PayloadRevurderAutomatiskPersonerMedInntektsendringerTask): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(payload),
                properties =
                    Properties().apply {
                        this["personIdent"] = payload.personIdent
                    },
            )
    }
}

data class PayloadRevurderAutomatiskPersonerMedInntektsendringerForvaltningTask(
    val personIdent: String,
    val harIngenEksisterendeYtelser: Boolean,
    val yearMonthProssesertTid: YearMonth,
)
