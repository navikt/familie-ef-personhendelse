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
    private val inntektsendringService: InntektsendringerService,
) : AsyncTaskStep {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        val (personIdentMedYtelser, årMåned) = objectMapper.readValue<PayloadRevurderAutomatiskPersonerMedInntektsendringerTask>(task.payload)
        secureLogger.info("Revurderer automatisk person med inntektsendringer: ${task.payload}")
        val personIdenterForRevurdering =
            personIdentMedYtelser
                .filter { personMedYtelse ->
                    val inntektResponse =
                        inntektClient.hentInntekt(
                            personMedYtelse.personIdent,
                            årMåned.minusMonths(3),
                            årMåned.minusMonths(1),
                        )
                    val harStabilInntekt =
                        inntektsendringService.harStabilInntektOgLoggInntekt(
                            inntektResponse,
                            årMåned,
                            personMedYtelse.personIdent,
                            personMedYtelse.harIngenEksisterendeYtelser,
                        )
                    harStabilInntekt && personMedYtelse.harIngenEksisterendeYtelser
                }.map { it.personIdent }

        if (personIdenterForRevurdering.isNotEmpty()) {
            sakClient.revurderAutomatisk(personIdenterForRevurdering)
        }
    }

    companion object {
        const val TYPE = "revurderAutomatiskPersonerMedInntektsendringerTask"

        fun opprettTask(payload: PayloadRevurderAutomatiskPersonerMedInntektsendringerTask): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(payload),
                properties =
                    Properties().apply {
                        this["årMåned"] = payload.årMåned.toString()
                    },
            )
    }
}

data class PayloadRevurderAutomatiskPersonerMedInntektsendringerTask(
    val personIdenterMedYtelser: List<PersonIdentMedYtelser>,
    val årMåned: YearMonth,
)

data class PersonIdentMedYtelser(
    val personIdent: String,
    val harIngenEksisterendeYtelser: Boolean,
)
