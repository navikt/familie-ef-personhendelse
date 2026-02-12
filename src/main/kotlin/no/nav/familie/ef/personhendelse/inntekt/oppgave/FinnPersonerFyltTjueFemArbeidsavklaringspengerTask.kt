package no.nav.familie.ef.personhendelse.inntekt.oppgave

import no.nav.familie.ef.personhendelse.client.pdl.PdlClient
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.readValue
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
        val (inntektsendringForBrukereMedArbeidsavklaringspenger, årMåned) = jsonMapper.readValue<PayloadFinnPersonerFyltTjueFemArbeidsavklaringspengerTask>(task.payload)
        val startDato = årMåned.minusMonths(1).atDay(6)
        val sluttDato = årMåned.atDay(7)

        val personerFylt25Aar =
            inntektsendringForBrukereMedArbeidsavklaringspenger.mapNotNull { personIdent ->
                val person = pdlClient.hentPerson(personIdent)
                val foedselsdato = person.foedselsdato.first().foedselsdato
                if (foedselsdato == null) {
                    secureLogger.error("Fant ingen fødselsdato for person $personIdent")
                }
                val fylte25Dato = foedselsdato?.plusYears(25)
                if (fylte25Dato?.isAfter(startDato) == true && fylte25Dato.isBefore(sluttDato)) personIdent else null
            }
        personerFylt25Aar.forEach { kandidat ->
            val payload = PayloadOpprettOppgaverForArbeidsavklaringspengerEndringerTask(personIdent = kandidat, årMåned = YearMonth.from(årMåned))
            val skalOppretteTask = taskService.finnTaskMedPayloadOgType(jsonMapper.writeValueAsString(payload), OpprettOppgaverForArbeidsavklaringspengerEndringerTask.TYPE) == null

            if (skalOppretteTask) {
                val task = OpprettOppgaverForArbeidsavklaringspengerEndringerTask.opprettTask(payload)
                taskService.save(task)
            }
        }
        logger.info("Funnet ${personerFylt25Aar.size} som har fylt 25 år med AAP av totalt ${inntektsendringForBrukereMedArbeidsavklaringspenger.size} med AAP")
    }

    companion object {
        const val TYPE = "finnPersonerFyltTjueFemArbeidsavklaringspengerTask"

        fun opprettTask(payload: PayloadFinnPersonerFyltTjueFemArbeidsavklaringspengerTask): Task =
            Task(
                type = TYPE,
                payload = jsonMapper.writeValueAsString(payload),
            )
    }
}

data class PayloadFinnPersonerFyltTjueFemArbeidsavklaringspengerTask(
    val personIdenterBrukereMedArbeidsavklaringspenger: List<String>,
    val årMåned: YearMonth,
)
