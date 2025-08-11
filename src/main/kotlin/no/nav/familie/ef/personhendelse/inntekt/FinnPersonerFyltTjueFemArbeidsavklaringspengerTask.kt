package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.personhendelse.client.pdl.PdlClient
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
@TaskStepBeskrivelse(
    taskStepType = FinnPersonerFyltTjueFemArbeidsavklaringspengerTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Finner personer med som mottar arbeidsavklaringspenger og har fylt 25 den siste måneden.",
)
class FinnPersonerFyltTjueFemArbeidsavklaringspengerTask(
    private val pdlClient: PdlClient,
    private val taskService: TaskService,
) : AsyncTaskStep {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        val (inntektsendringForBrukereMedArbeidsavklaringspenger, årMåned) = objectMapper.readValue<PayloadFinnPersonerFyltTjueFemArbeidsavklaringspengerTask>(task.payload)
        val startDato = årMåned.minusMonths(1).atDay(6)
        val sluttDato = årMåned

        val kandidater =
            inntektsendringForBrukereMedArbeidsavklaringspenger.mapNotNull { personIdent ->
                val person = pdlClient.hentPerson(personIdent)
                val foedselsdato = person.foedselsdato.first().foedselsdato
                if (foedselsdato == null) {
                    secureLogger.error("Fant ingen fødselsdato for person $personIdent")
                }
                val fylte25Dato = foedselsdato?.plusYears(25)
                if (fylte25Dato?.isAfter(startDato) == true && fylte25Dato.isBefore(sluttDato.atDay(7))) personIdent else null
            }
        kandidater.forEach { kandidat ->
            val payload = PayloadOpprettOppgaverForArbeidsavklaringspengerEndringerTask(personIdent = kandidat, årMåned = YearMonth.from(årMåned))
            val skalOppretteTask = taskService.finnTaskMedPayloadOgType(objectMapper.writeValueAsString(payload), OpprettOppgaverForArbeidsavklaringspengerEndringerTask.TYPE) == null

            if (skalOppretteTask) {
                val task = OpprettOppgaverForArbeidsavklaringspengerEndringerTask.opprettTask(payload)
                taskService.save(task)
            }
        }
    }

    companion object {
        const val TYPE = "finnPersonerFyltTjueFemArbeidsavklaringspengerTask"

        fun opprettTask(payload: PayloadFinnPersonerFyltTjueFemArbeidsavklaringspengerTask): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(payload),
            )
    }
}

data class PayloadFinnPersonerFyltTjueFemArbeidsavklaringspengerTask(
    val personIdenterBrukereMedArbeidsavklaringspenger: List<String>,
    val årMåned: YearMonth,
)
