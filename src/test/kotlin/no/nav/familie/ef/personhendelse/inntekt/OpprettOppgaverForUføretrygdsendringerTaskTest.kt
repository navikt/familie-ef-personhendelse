package no.nav.familie.ef.personhendelse.inntekt

import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

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
        val payload = """{"personIdent":"123", "årMåned":"6"}"""
        val task = OpprettOppgaverForUføretrygdsendringerTask.opprettTask(payload)
        taskService.save(task)
        val taskList = taskService.findAll()
        val taskFraDB = taskList.first()
        assertThat(taskFraDB.metadata).isNotEmpty
        assertThat(taskFraDB.metadataWrapper.properties.keys.size).isEqualTo(3)
        assertThat(taskFraDB.metadataWrapper.properties.keys).contains("personIdent", "årMåned", "callId")
        opprettOppgaverForUføretrygdsendringerTask.doTask(task)
        verify(exactly = 1) { inntektOppgaveService.opprettOppgaveForUføretrygdEndring(any(), any()) }
    }
}
