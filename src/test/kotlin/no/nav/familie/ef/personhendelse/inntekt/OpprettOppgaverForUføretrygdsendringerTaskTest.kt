package no.nav.familie.ef.personhendelse.inntekt

import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.YearMonth

class OpprettOppgaverForUføretrygdsendringerTaskTest : IntegrasjonSpringRunnerTest() {
    private val inntektOppgaveService = mockk<InntektOppgaveService>(relaxed = true)

    @Autowired
    private lateinit var taskService: TaskService

    private lateinit var opprettOppgaverForUføretrygdsendringerTask: OpprettOppgaverForUføretrygdsendringerTask

    @BeforeEach
    fun setup() {
        opprettOppgaverForUføretrygdsendringerTask =
            OpprettOppgaverForUføretrygdsendringerTask(
                inntektOppgaveService = inntektOppgaveService,
            )
    }

    @Test
    fun `Sjekk at man kan opprette task for uføretrygdsendringer og at den har riktig metadata`() {
        val payload =
            PayloadOpprettOppgaverForUføretrygdsendringerTask(
                personIdent = "123",
                årMåned = YearMonth.of(2023, 10),
            )

        val task = OpprettOppgaverForUføretrygdsendringerTask.opprettTask(payload)
        taskService.save(task)
        val taskList = taskService.findAll()
        val taskFraDB = taskList.get(taskList.size - 1)
        assertThat(taskFraDB.metadata).isNotEmpty
        assertThat(taskFraDB.metadataWrapper.properties.keys.size).isEqualTo(3)
        assertThat(taskFraDB.metadataWrapper.properties.keys).contains("personIdent", "årMåned", "callId")
        opprettOppgaverForUføretrygdsendringerTask.doTask(task)
        verify(exactly = 1) { inntektOppgaveService.opprettOppgaveForUføretrygdEndring(any(), any()) }
    }
}
