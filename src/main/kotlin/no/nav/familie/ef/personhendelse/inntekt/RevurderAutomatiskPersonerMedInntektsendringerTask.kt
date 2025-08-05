package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.personhendelse.client.SakClient
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
    taskStepType = RevurderAutomatiskPersonerMedInntektsendringerTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Revurder automatisk person med inntektsendringer",
)
class RevurderAutomatiskPersonerMedInntektsendringerTask(
    private val sakClient: SakClient,
    private val inntektClient: InntektClient,
) : AsyncTaskStep {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        val (personIdent, harIngenEksisterendeYtelser, yearMonthProssesertTid) = objectMapper.readValue<PayloadRevurderAutomatiskPersonerMedInntektsendringerTask>(task.payload)
        secureLogger.info("Reverdurer automatisk person med inntektsendringer: ${task.payload}")
        val inntektResponse = inntektClient.hentInntekt(personIdent, yearMonthProssesertTid.minusMonths(3), yearMonthProssesertTid.minusMonths(1))
        val totalInntektTreMånederTilbake = inntektResponse.totalInntektForÅrMånedUtenFeriepenger(yearMonthProssesertTid.minusMonths(3))
        val totalInntektToMånederTilbake = inntektResponse.totalInntektForÅrMånedUtenFeriepenger(yearMonthProssesertTid.minusMonths(2))
        val totalInntektForrigeMåned = inntektResponse.totalInntektForÅrMånedUtenFeriepenger(yearMonthProssesertTid.minusMonths(1))

        val harStabilInntekt = abs(totalInntektTreMånederTilbake - totalInntektToMånederTilbake) < 3000 && abs(totalInntektTreMånederTilbake - totalInntektForrigeMåned) < 3000
        if (harStabilInntekt && harIngenEksisterendeYtelser) {
            sakClient.revurderAutomatisk(listOf<String>(personIdent))
        }
        secureLogger.info("Total inntekt pr mnd uten feriepenger ${personIdent}: $totalInntektTreMånederTilbake, $totalInntektToMånederTilbake, $totalInntektForrigeMåned. Har stabil inntekt: $harStabilInntekt - eksisterende ytelser: $harIngenEksisterendeYtelser")
    }



    companion object {
        const val TYPE = "revurderAutomatiskPersonerMedInntektsendringerTask"

        fun opprettTask(payload: String): Task {
            val payloadObject = objectMapper.readValue(payload, PayloadRevurderAutomatiskPersonerMedInntektsendringerTask::class.java)

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

data class PayloadRevurderAutomatiskPersonerMedInntektsendringerTask(
    val personIdent: String,
    val harIngenEksisterendeYtelser: Boolean,
    val yearMonthProssesertTid: YearMonth,
)