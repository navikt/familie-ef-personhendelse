package no.nav.familie.ef.personhendelse.inntekt

import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.ef.personhendelse.inntekt.oppgave.InntektOppgaveService
import no.nav.familie.ef.personhendelse.inntekt.oppgave.OpprettOppgaverForNyeVedtakUføretrygdTask
import no.nav.familie.ef.personhendelse.inntekt.oppgave.PayloadOpprettOppgaverForNyeVedtakUføretrygdTask
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.YearMonth

class OpprettOppgaverForNyeVedtakUføretrygdTaskTest : IntegrasjonSpringRunnerTest() {
    private val inntektOppgaveService = mockk<InntektOppgaveService>(relaxed = true)

    @Autowired
    private lateinit var taskService: TaskService

    private lateinit var opprettOppgaverForNyeVedtakUføretrygdTask: OpprettOppgaverForNyeVedtakUføretrygdTask

    @BeforeEach
    fun setup() {
        opprettOppgaverForNyeVedtakUføretrygdTask =
            OpprettOppgaverForNyeVedtakUføretrygdTask(
                inntektOppgaveService = inntektOppgaveService,
            )
    }

    @Test
    fun `Sjekk at man kan opprette task for uføretrygdsendringer og at den har riktig metadata`() {
        val payload =
            PayloadOpprettOppgaverForNyeVedtakUføretrygdTask(
                personIdent = "123",
                prosessertYearMonth = YearMonth.of(2023, 10),
            )
        val task = OpprettOppgaverForNyeVedtakUføretrygdTask.opprettTask(payload)
        taskService.save(task)
        val taskListOpprettOppgaveTask = taskService.finnAlleTaskerMedType(OpprettOppgaverForNyeVedtakUføretrygdTask.TYPE)
        assertThat(taskListOpprettOppgaveTask).hasSize(1)
        val taskFraDB = taskListOpprettOppgaveTask.first()
        assertThat(taskFraDB.metadata).isNotEmpty
        assertThat(taskFraDB.metadataWrapper.properties.keys.size).isEqualTo(2)
        assertThat(taskFraDB.metadataWrapper.properties.keys).contains("personIdent", "callId")
        opprettOppgaverForNyeVedtakUføretrygdTask.doTask(task)
        verify(exactly = 1) { inntektOppgaveService.opprettOppgaveForInntektsendring(any(), any()) }
    }
}
