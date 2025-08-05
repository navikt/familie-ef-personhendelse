package no.nav.familie.ef.personhendelse.inntekt

import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class RevurderAutomatiskPersonerMedInntektsendringerTaskTest : IntegrasjonSpringRunnerTest() {
    private val sakClient = mockk<SakClient>(relaxed = true)
    private val inntektClient = mockk<InntektClient>(relaxed = true)

    @Autowired
    private lateinit var taskService: TaskService

    private lateinit var revurderAutomatiskPersonerMedInntektsendringerTask: RevurderAutomatiskPersonerMedInntektsendringerTask

    @BeforeEach
    fun setup() {
        revurderAutomatiskPersonerMedInntektsendringerTask =
            RevurderAutomatiskPersonerMedInntektsendringerTask(
                sakClient = sakClient,
                inntektClient = inntektClient,
            )
    }

    @Test
    fun `Sjekk at man kan opprette task for uføretrygdsendringer og at den har riktig metadata`() {
        val payload = """{"personIdent":"123", "harIngenEksisterendeYtelser":"true"}"""
        val task = RevurderAutomatiskPersonerMedInntektsendringerTask.opprettTask(payload)
        taskService.save(task)
        val taskList = taskService.findAll()
        val taskFraDB = taskList.get(taskList.size - 1)
        assertThat(taskFraDB.metadata).isNotEmpty
        assertThat(taskFraDB.metadataWrapper.properties.keys.size).isEqualTo(2)
        assertThat(taskFraDB.metadataWrapper.properties.keys).contains("personIdent", "callId")
        revurderAutomatiskPersonerMedInntektsendringerTask.doTask(task)
        verify(exactly = 1) { sakClient.revurderAutomatisk(any()) }
    }
}
