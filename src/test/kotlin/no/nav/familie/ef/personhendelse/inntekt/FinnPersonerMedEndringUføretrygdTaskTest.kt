package no.nav.familie.ef.personhendelse.inntekt
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.ef.personhendelse.util.JsonFilUtil.Companion.readResource
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
        val personIdenter = "123"
        val payload =
            PayloadFinnPersonerMedEndringUføretrygdTask(
                personIdenter = listOf(personIdenter),
                årMåned = YearMonth.now(),
            )

        val task = FinnPersonerMedEndringUføretrygdTask.opprettTask(payload)
        taskService.save(task)
        finnPersonerMedEndringUføretrygdTask.doTask(task)
        val taskListFinnPersonerTask = taskService.finnAlleTaskerMedType(FinnPersonerMedEndringUføretrygdTask.TYPE)
        val taskListOpprettOppgaveTask = taskService.finnAlleTaskerMedType(OpprettOppgaverForUføretrygdsendringerTask.TYPE)
        assertThat(taskListFinnPersonerTask).hasSize(1)
        assertThat(taskListOpprettOppgaveTask).hasSize(1)
        val taskFraDBFinnPerson = taskListFinnPersonerTask.first()
        val taskFraDBLagOppgave = taskListOpprettOppgaveTask.first()
        assertThat(taskFraDBFinnPerson.metadata).isNotEmpty
        assertThat(taskFraDBFinnPerson.metadataWrapper.properties.keys.size).isEqualTo(1)
        assertThat(taskFraDBFinnPerson.metadataWrapper.properties.keys).contains("callId")
        assertThat(taskFraDBLagOppgave.metadata).isNotEmpty
        assertThat(taskFraDBLagOppgave.metadataWrapper.properties.keys.size).isEqualTo(3)
        assertThat(taskFraDBLagOppgave.metadataWrapper.properties.keys).contains("callId", "personIdent", "årMåned")
    }

    @Test
    fun `Sjekk uføretrygd endring hvor det finnes flere innslag av inntekt på samme måned`() {
        val json: String = readResource("inntekt/InntektUføretrygdUtenEndring.json")
        val inntektResponse = objectMapper.readValue<InntektResponse>(json)

        every { inntektsendringerService.hentInntekt(any()) } returns inntektResponse

        val personIdenter = "123"
        val payload =
            PayloadFinnPersonerMedEndringUføretrygdTask(
                personIdenter = listOf(personIdenter),
                årMåned = YearMonth.now(),
            )
        val task = FinnPersonerMedEndringUføretrygdTask.opprettTask(payload)
        taskService.save(task)
        finnPersonerMedEndringUføretrygdTask.doTask(task)

        assertThat(taskService.findAll().filter { it.type == OpprettOppgaverForUføretrygdsendringerTask.TYPE }.size).isEqualTo(0)
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
