package no.nav.familie.ef.personhendelse.inntekt

import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

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
        val payload = """{"personIdent":"123", "totalFeilutbetaling":"5000", "yearMonthProssesertTid":"2023-10"}"""
        val task = OpprettOppgaverForInntektsendringerTask.opprettTask(payload)
        taskService.save(task)
        val taskList = taskService.findAll()
        val taskFraDB = taskList.get(taskList.size - 1)
        assertThat(taskFraDB.metadata).isNotEmpty
        assertThat(taskFraDB.metadataWrapper.properties.keys.size).isEqualTo(2)
        assertThat(taskFraDB.metadataWrapper.properties.keys).contains("personIdent", "callId")
        opprettOppgaverForInntektsendringerTask.doTask(task)
        verify(exactly = 1) { inntektOppgaveService.opprettOppgaveForInntektsendring(any(), any()) }
    }
}
