package no.nav.familie.ef.personhendelse.inntekt

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.ef.personhendelse.client.ForventetInntektForPerson
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.YearMonth

class BeregnInntektsendringerOgLagreIDbTaskTest : IntegrasjonSpringRunnerTest() {
    private val inntektsendringerService = mockk<InntektsendringerService>(relaxed = true)
    private val sakClient = mockk<SakClient>(relaxed = true)

    @Autowired
    private lateinit var taskService: TaskService

    private lateinit var beregnInntektsendringerOgLagreIDbTask: BeregnInntektsendringerOgLagreIDbTask

    @BeforeEach
    fun setup() {
        beregnInntektsendringerOgLagreIDbTask =
            BeregnInntektsendringerOgLagreIDbTask(
                inntektsendringerService = inntektsendringerService,
                sakClient = sakClient,
            )
    }

    @Test
    fun `Sjekk at man kan opprette task for uføretrygdsendringer og at den har riktig metadata`() {
        every { sakClient.hentForventetInntektForIdenter(any()) } returns listOf(ForventetInntektForPerson("123", 10_000, 10_000, 10_000, 10_000))
        val payload =
            PayloadBeregnInntektsendringerOgLagreIDbTask(
                personIdent = "123",
                yearMonth = YearMonth.of(2023, 10),
            )
        val jsonPayload = objectMapper.writeValueAsString(payload)
        val task = BeregnInntektsendringerOgLagreIDbTask.opprettTask(jsonPayload)
        taskService.save(task)
        val taskList = taskService.findAll()
        val taskFraDB = taskList.get(taskList.size - 1)
        assertThat(taskFraDB.metadata).isNotEmpty
        assertThat(taskFraDB.metadataWrapper.properties.keys.size).isEqualTo(2)
        assertThat(taskFraDB.metadataWrapper.properties.keys).contains("personIdent", "callId")
        beregnInntektsendringerOgLagreIDbTask.doTask(task)
        verify(exactly = 1) { inntektsendringerService.lagreInntektsendringForPerson(any(), any()) }
    }
}
