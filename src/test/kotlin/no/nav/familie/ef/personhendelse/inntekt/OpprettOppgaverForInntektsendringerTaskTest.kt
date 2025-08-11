package no.nav.familie.ef.personhendelse.inntekt

import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.YearMonth

class OpprettOppgaverForInntektsendringerTaskTest : IntegrasjonSpringRunnerTest() {
    private val inntektOppgaveService = mockk<InntektOppgaveService>(relaxed = true)

    @Autowired
    private lateinit var taskService: TaskService

    private lateinit var opprettOppgaverForInntektsendringerTask: OpprettOppgaverForInntektsendringerTask

    @BeforeEach
    fun setup() {
        opprettOppgaverForInntektsendringerTask =
            OpprettOppgaverForInntektsendringerTask(
                inntektOppgaveService = inntektOppgaveService,
            )
    }

    @Test
    fun `Sjekk at man kan opprette task for arbeidsavklaringspenger endringer og at den har riktig metadata`() {
        val payload = PayloadOpprettOppgaverForInntektsendringerTask("123", 123, YearMonth.now())
        val task = OpprettOppgaverForInntektsendringerTask.opprettTask(payload)
        taskService.save(task)
        val taskListOpprettOppgaveTask = taskService.finnAlleTaskerMedType(OpprettOppgaverForInntektsendringerTask.TYPE)
        val taskFraDB = taskListOpprettOppgaveTask.first()
        assertThat(taskFraDB.metadata).isNotEmpty
        assertThat(taskFraDB.metadataWrapper.properties.keys.size).isEqualTo(2)
        assertThat(taskFraDB.metadataWrapper.properties.keys).contains("personIdent", "callId")
        opprettOppgaverForInntektsendringerTask.doTask(task)
        verify(exactly = 1) { inntektOppgaveService.opprettOppgaveForInntektsendring(any(), any()) }
    }
}
