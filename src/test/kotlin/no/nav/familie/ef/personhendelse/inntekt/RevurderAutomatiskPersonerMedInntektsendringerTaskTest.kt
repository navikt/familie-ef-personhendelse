package no.nav.familie.ef.personhendelse.inntekt

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.inntekt.InntektClientTest.Companion.lesRessurs
import no.nav.familie.ef.personhendelse.inntekt.endring.InntektsendringerService
import no.nav.familie.ef.personhendelse.inntekt.endring.RevurderAutomatiskPersonerMedInntektsendringerTask
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.module.kotlin.readValue

class RevurderAutomatiskPersonerMedInntektsendringerTaskTest : IntegrasjonSpringRunnerTest() {
    private val sakClient = mockk<SakClient>(relaxed = true)
    private val inntektClient = mockk<InntektClient>(relaxed = true)

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var inntektsendringService: InntektsendringerService

    private lateinit var revurderAutomatiskPersonerMedInntektsendringerTask: RevurderAutomatiskPersonerMedInntektsendringerTask

    @BeforeEach
    fun setup() {
        revurderAutomatiskPersonerMedInntektsendringerTask =
            RevurderAutomatiskPersonerMedInntektsendringerTask(
                sakClient = sakClient,
                inntektClient = inntektClient,
                inntektsendringService = inntektsendringService,
            )
    }

    @Test
    fun `Sjekk at man kan opprette task for uføretrygdsendringer og at den har riktig metadata`() {
        val payload = listOf("123")
        val task = RevurderAutomatiskPersonerMedInntektsendringerTask.opprettTask(payload)
        val inntektV2ResponseJson: String = lesRessurs("inntekt/InntektGenerellResponse.json")
        val inntektResponse = jsonMapper.readValue<InntektResponse>(inntektV2ResponseJson)
        every { inntektClient.hentInntekt(any(), any(), any()) } returns inntektResponse
        taskService.save(task)
        val taskList = taskService.finnAlleTaskerMedType(RevurderAutomatiskPersonerMedInntektsendringerTask.TYPE)
        assertThat(taskList).hasSize(1)
        val taskFraDB = taskList.first()
        assertThat(taskFraDB.metadata).isNotEmpty
        assertThat(taskFraDB.metadataWrapper.properties.keys.size).isEqualTo(1)
        assertThat(taskFraDB.metadataWrapper.properties.keys).contains("callId")
        revurderAutomatiskPersonerMedInntektsendringerTask.doTask(task)
        verify(exactly = 1) { sakClient.revurderAutomatisk(any()) }
    }
}
