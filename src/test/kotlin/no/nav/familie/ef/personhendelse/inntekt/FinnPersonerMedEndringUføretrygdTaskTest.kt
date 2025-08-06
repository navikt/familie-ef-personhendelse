package no.nav.familie.ef.personhendelse.inntekt

import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth

class FinnPersonerMedEndringUføretrygdTaskTest : IntegrasjonSpringRunnerTest() {
    private val inntektsendringerService = mockk<InntektsendringerService>(relaxed = true)
    private val taskService = mockk<TaskService>(relaxed = true)

    private lateinit var finnPersonerMedEndringUføretrygdTask: FinnPersonerMedEndringUføretrygdTask

    @BeforeEach
    fun setup() {
        finnPersonerMedEndringUføretrygdTask =
            FinnPersonerMedEndringUføretrygdTask(
                inntektsendringerService = inntektsendringerService,
                taskService = taskService,
            )
    }

    @Test
    fun `Sjekk at man oppretter oppgave dersom person har fått høyere beløp siste to måneder uføretrygd`() {
        val inntektsendring =
            InntektOgVedtakEndring(
                personIdent = "12345",
                harNyeVedtak = false,
                prosessertTid = LocalDateTime.now(),
                inntektsendringFireMånederTilbake = BeregningResultat(1000, 0, 0),
                inntektsendringTreMånederTilbake = BeregningResultat(1000, 0, 0),
                inntektsendringToMånederTilbake = BeregningResultat(1000, 0, 0),
                inntektsendringForrigeMåned = BeregningResultat(1200, 10, 200),
                nyeYtelser = null,
                eksisterendeYtelser = "uføretrygd",
            )
        val payload =
            PayloadFinnPersonerMedEndringUføretrygdTask(
                inntektsendringForBrukereMedUføretrygd = listOf(inntektsendring),
                årMåned = YearMonth.now(),
            )
        val payloadJson = objectMapper.writeValueAsString(payload)

        val task = FinnPersonerMedEndringUføretrygdTask.opprettTask(payloadJson)
        taskService.save(task)
        val taskList = taskService.findAll()
        val taskFraDB = taskList.first()
        assertThat(taskFraDB.metadata).isNotEmpty
        assertThat(taskFraDB.metadataWrapper.properties.keys.size).isEqualTo(2)
        assertThat(taskFraDB.metadataWrapper.properties.keys).contains("årMåned", "callId")
        finnPersonerMedEndringUføretrygdTask.doTask(task)
        verify(exactly = 1) { taskService.save(any()) }
    }
}
