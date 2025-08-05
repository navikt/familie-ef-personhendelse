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

@Service
@TaskStepBeskrivelse(
    taskStepType = BeregnInntektsendringerOgLagreIDbTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Lagrer inntektsendringer i databasen.",
)
class BeregnInntektsendringerOgLagreIDbTask(
    private val inntektsendringerService: InntektsendringerService,
    private val sakClient: SakClient,
) : AsyncTaskStep {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        val personIdent = objectMapper.readValue<PayloadBeregnInntektsendringerOgLagreIDbTask>(task.payload).personIdent
        secureLogger.info("Oppretter oppgaver for arbeidsavklaringspenger endringer ${task.payload}")
        sakClient.hentForventetInntektForIdenter(listOf(personIdent)).forEach { forventetInntektForPerson ->
            val inntektResponse = inntektsendringerService.hentInntekt(personIdent = forventetInntektForPerson.personIdent)

            if (inntektResponse != null && forventetInntektForPerson.erSiste2MånederNotNull()) {
                inntektsendringerService.lagreInntektsendringForPerson(
                    forventetInntektForPerson = forventetInntektForPerson,
                    inntektResponse = inntektResponse,
                )
            }
        }
    }

    companion object {
        const val TYPE = "BeregnInntektsendringerOgLagreIDbTask"

        fun opprettTask(payload: String): Task {
            val payloadObject = objectMapper.readValue(payload, PayloadBeregnInntektsendringerOgLagreIDbTask::class.java)

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

data class PayloadBeregnInntektsendringerOgLagreIDbTask(
    val personIdent: String,
    val yearMonth: YearMonth,
)
