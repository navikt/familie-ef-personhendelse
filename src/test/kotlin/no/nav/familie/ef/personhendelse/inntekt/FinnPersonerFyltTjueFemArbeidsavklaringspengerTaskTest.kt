package no.nav.familie.ef.personhendelse.inntekt

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.ef.personhendelse.client.pdl.PdlClient
import no.nav.familie.ef.personhendelse.generated.hentperson.Foedselsdato
import no.nav.familie.ef.personhendelse.generated.hentperson.Person
import no.nav.familie.ef.personhendelse.inntekt.oppgave.FinnPersonerFyltTjueFemArbeidsavklaringspengerTask
import no.nav.familie.ef.personhendelse.inntekt.oppgave.OpprettOppgaverForArbeidsavklaringspengerEndringerTask
import no.nav.familie.ef.personhendelse.inntekt.oppgave.PayloadFinnPersonerFyltTjueFemArbeidsavklaringspengerTask
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

class FinnPersonerFyltTjueFemArbeidsavklaringspengerTaskTest : IntegrasjonSpringRunnerTest() {
    private val pdlClient = mockk<PdlClient>(relaxed = true)

    @Autowired
    private lateinit var taskService: TaskService

    private lateinit var finnPersonerFyltTjueFemArbeidsavklaringspengerTask: FinnPersonerFyltTjueFemArbeidsavklaringspengerTask

    @BeforeEach
    fun setup() {
        finnPersonerFyltTjueFemArbeidsavklaringspengerTask =
            FinnPersonerFyltTjueFemArbeidsavklaringspengerTask(
                pdlClient = pdlClient,
                taskService = taskService,
            )
    }

    @Test
    fun `Sjekk at man oppretter oppgave dersom person har fylt 25`() {
        val person =
            Person(
                forelderBarnRelasjon = emptyList(),
                statsborgerskap = emptyList(),
                sivilstand = emptyList(),
                adressebeskyttelse = emptyList(),
                bostedsadresse = emptyList(),
                doedsfall = emptyList(),
                foedselsdato =
                    listOf(
                        Foedselsdato(
                            foedselsdato = LocalDate.of(YearMonth.now().minusYears(25).year, YearMonth.now().month, 1),
                        ),
                    ),
            )
        every { pdlClient.hentPerson(any()) } returns person
        val personIdenter = "123"
        val payload =
            PayloadFinnPersonerFyltTjueFemArbeidsavklaringspengerTask(
                personIdenterBrukereMedArbeidsavklaringspenger = listOf(personIdenter),
                årMåned = YearMonth.now(),
            )

        val task = FinnPersonerFyltTjueFemArbeidsavklaringspengerTask.opprettTask(payload)
        taskService.save(task)
        finnPersonerFyltTjueFemArbeidsavklaringspengerTask.doTask(task)
        val taskListFinnPersonerTask =
            taskService.finnAlleTaskerMedType(FinnPersonerFyltTjueFemArbeidsavklaringspengerTask.TYPE)
        val taskListOpprettOppgaveTask =
            taskService.finnAlleTaskerMedType(OpprettOppgaverForArbeidsavklaringspengerEndringerTask.TYPE)
        assertThat(taskListFinnPersonerTask).hasSize(1)
        assertThat(taskListOpprettOppgaveTask).hasSize(1)
        val taskFraDBFinnPerson = taskListFinnPersonerTask.first()
        val taskFraDBLagOppgave = taskListOpprettOppgaveTask.first()
        assertThat(taskFraDBFinnPerson.metadata).isNotEmpty
        assertThat(taskFraDBFinnPerson.metadataWrapper.properties.keys.size).isEqualTo(1)
        assertThat(taskFraDBFinnPerson.metadataWrapper.properties.keys).contains("callId")
        assertThat(taskFraDBLagOppgave.metadata).isNotEmpty
        assertThat(taskFraDBLagOppgave.metadataWrapper.properties.keys.size).isEqualTo(3)
        assertThat(taskFraDBLagOppgave.metadataWrapper.properties.keys).contains("callId", "personIdent", "årMåned")
    }
}
