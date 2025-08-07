package no.nav.familie.ef.personhendelse.inntekt
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth

class FinnPersonerMedEndringUføretrygdTaskTest : IntegrasjonSpringRunnerTest() {
    private val inntektsendringerService = mockk<InntektsendringerService>(relaxed = true)

    @Autowired
    private lateinit var taskService: TaskService

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
        val inntekt =
            lagInntektsResponseMedUføretrygdOgGittBeløpToSisteMåneder(
                belopForrigeMaaned = 1200.0,
                belopToMndTilbake = 1000.0,
            )
        every { inntektsendringerService.hentInntekt(any()) } returns inntekt
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
                eksisterendeYtelser = "ufoeretrygd",
            )
        val payload =
            PayloadFinnPersonerMedEndringUføretrygdTask(
                inntektsendringForBrukereMedUføretrygd = listOf(inntektsendring),
                årMåned = YearMonth.now(),
            )
        val payloadJson = objectMapper.writeValueAsString(payload)

        val task = FinnPersonerMedEndringUføretrygdTask.opprettTask(payloadJson)
        taskService.save(task)
        finnPersonerMedEndringUføretrygdTask.doTask(task)
        val taskList = taskService.findAll()
        val taskFraDBFinnPerson = taskList.get(taskList.size - 2)
        val taskFraDBLagOppgave = taskList.get(taskList.size - 1)
        assertThat(taskFraDBFinnPerson.metadata).isNotEmpty
        assertThat(taskFraDBFinnPerson.metadataWrapper.properties.keys.size).isEqualTo(1)
        assertThat(taskFraDBFinnPerson.metadataWrapper.properties.keys).contains("callId")
        assertThat(taskFraDBLagOppgave.metadata).isNotEmpty
        assertThat(taskFraDBLagOppgave.metadataWrapper.properties.keys.size).isEqualTo(3)
        assertThat(taskFraDBLagOppgave.metadataWrapper.properties.keys).contains("callId", "personIdent", "årMåned")
    }
}

private fun lagInntektsResponseMedUføretrygdOgGittBeløpToSisteMåneder(
    belopForrigeMaaned: Double,
    belopToMndTilbake: Double,
): InntektResponse =
    InntektResponse(
        inntektsmåneder =
            listOf(
                Inntektsmåned(
                    måned = YearMonth.now().minusMonths(1),
                    opplysningspliktig = "123",
                    underenhet = "123",
                    norskident = "123",
                    oppsummeringstidspunkt = OffsetDateTime.now(),
                    inntektListe =
                        listOf(
                            Inntekt(
                                type = InntektType.YTELSE_FRA_OFFENTLIGE,
                                beløp = belopForrigeMaaned,
                                fordel = "123",
                                beskrivelse = "ufoeretrygd",
                                inngårIGrunnlagForTrekk = true,
                                utløserArbeidsgiveravgift = true,
                                skatteOgAvgiftsregel = null,
                                opptjeningsperiodeFom = null,
                                opptjeningsperiodeTom = LocalDate.now(),
                                tilleggsinformasjon = null,
                                manuellVurdering = true,
                                antall = null,
                                skattemessigBosattLand = null,
                                opptjeningsland = null,
                            ),
                        ),
                ),
                Inntektsmåned(
                    måned = YearMonth.now().minusMonths(2),
                    opplysningspliktig = "123",
                    underenhet = "123",
                    norskident = "123",
                    oppsummeringstidspunkt = OffsetDateTime.now(),
                    inntektListe =
                        listOf(
                            Inntekt(
                                type = InntektType.YTELSE_FRA_OFFENTLIGE,
                                beløp = belopToMndTilbake,
                                fordel = "123",
                                beskrivelse = "ufoeretrygd",
                                inngårIGrunnlagForTrekk = true,
                                utløserArbeidsgiveravgift = true,
                                skatteOgAvgiftsregel = null,
                                opptjeningsperiodeFom = null,
                                opptjeningsperiodeTom = LocalDate.now(),
                                tilleggsinformasjon = null,
                                manuellVurdering = true,
                                antall = null,
                                skattemessigBosattLand = null,
                                opptjeningsland = null,
                            ),
                        ),
                ),
            ),
    )
