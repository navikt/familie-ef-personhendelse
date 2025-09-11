package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.personhendelse.client.ForventetInntektForPerson
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.inntekt.endring.BeregningResultat
import no.nav.familie.ef.personhendelse.inntekt.endring.Inntektsendring
import no.nav.familie.ef.personhendelse.inntekt.endring.InntektsendringerService
import no.nav.familie.ef.personhendelse.util.JsonFilUtil.Companion.lagInntektResponseFraJsonMedEnMåned
import no.nav.familie.ef.personhendelse.util.JsonFilUtil.Companion.readResource
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth

class InntektsendringerServiceTest {
    private val oppgaveClient = mockk<OppgaveClient>()
    private val sakClient = mockk<SakClient>()
    private val inntektsendringerRepository = mockk<InntektsendringerRepository>()
    private val inntektClient = mockk<InntektClient>()
    private val taskService = mockk<TaskService>()

    val inntektsendringerService = InntektsendringerService(oppgaveClient, sakClient, inntektsendringerRepository, inntektClient, taskService)
    val forventetÅrligInntekt = 420000 // 35k pr mnd i eksempel json-fil

    val enMndTilbake = YearMonth.now().minusMonths(1)

    @BeforeEach
    internal fun setUp() {
        every { sakClient.inntektForEksternId(1) } returns forventetÅrligInntekt
        every { sakClient.inntektForEksternId(2) } returns (forventetÅrligInntekt * 0.9).toInt()
        every { sakClient.inntektForEksternId(3) } returns (forventetÅrligInntekt * 0.91).toInt()
    }

    @Test
    fun `Har endret inntekt med mer enn 10 prosent i forhold til forventet inntekt`() {
        val json: String = readResource("inntekt/InntektLoennsinntektEksempel.json") // 40k

        val oppdatertInntektResponse = lagInntektResponseFraJsonMedEnMåned(json)

        val forventetInntektTiProsentLavere = (forventetÅrligInntekt * 0.9).toInt()
        val forventetInntektNiProsentLavere = (forventetÅrligInntekt * 0.91).toInt()

        Assertions
            .assertThat(
                inntektsendringerService.beregnEndretInntekt(
                    inntektResponse = oppdatertInntektResponse,
                    forventetInntektForPerson = forventetInntektForPerson(forventetÅrligInntekt),
                ),
            ).isEqualTo(inntektsendring())
        Assertions
            .assertThat(
                inntektsendringerService.beregnEndretInntekt(
                    inntektResponse = oppdatertInntektResponse,
                    forventetInntektForPerson = forventetInntektForPerson(forventetInntektTiProsentLavere),
                ),
            ).isEqualTo(inntektsendring(3500, 11, 1575))

        Assertions
            .assertThat(
                inntektsendringerService.beregnEndretInntekt(
                    inntektResponse = oppdatertInntektResponse,
                    forventetInntektForPerson = forventetInntektForPerson(forventetInntektNiProsentLavere),
                ),
            ).isEqualTo(inntektsendring(3150, 9, 1418))
    }

    @Test
    fun `Utbetaling av offentlig ytelse og lønnsinntekt utgjør til sammen mer enn 10 prosent av forventet inntekt`() {
        val json: String = readResource("inntekt/InntektLoennsinntektOgOffentligYtelseEksempel.json") // 38,5k totalt

        val oppdatertInntektResponse = lagInntektResponseFraJsonMedEnMåned(json)

        Assertions
            .assertThat(
                inntektsendringerService
                    .beregnEndretInntekt(
                        inntektResponse = oppdatertInntektResponse,
                        forventetInntektForPerson = forventetInntektForPerson(forventetÅrligInntekt),
                    ).harEndretInntekt(),
            ).isTrue
    }

    @Test
    fun `Etterbetaling skal ikke medberegnes`() {
        val json: String = readResource("inntekt/InntektEtterbetalingSkalIgnoreres.json") // Inntekt 35k + etterbetaling 10k

        val oppdatertInntektResponse = lagInntektResponseFraJsonMedEnMåned(json)

        Assertions
            .assertThat(
                inntektsendringerService
                    .beregnEndretInntekt(
                        inntektResponse = oppdatertInntektResponse,
                        forventetInntektForPerson = forventetInntektForPerson(forventetÅrligInntekt),
                    ).harEndretInntekt(),
            ).isFalse
    }

    @Test
    fun `Har for høy forventet inntekt, skal returnere false`() {
        val json: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val inntektResponse = objectMapper.readValue<InntektResponse>(json) // 40k pr mnd

        val arbeidsinntektMåned = inntektResponse.inntektsmåneder.firstOrNull() ?: Assertions.fail("Inntekt mangler")

        val oppdatertInntektResponse =
            inntektResponse.copy(
                inntektsmåneder =
                    listOf(
                        arbeidsinntektMåned.copy(måned = enMndTilbake),
                    ),
            )

        Assertions
            .assertThat(
                inntektsendringerService
                    .beregnEndretInntekt(
                        inntektResponse = oppdatertInntektResponse,
                        forventetInntektForPerson = forventetInntektForPerson(forventetÅrligInntekt),
                    ).harEndretInntekt(),
            ).isFalse
    }

    @Test
    fun `Har inntekt under halv G, skal returnere false selv om inntekt har økt mer enn 10 prosent`() {
        val json: String = readResource("inntekt/InntektUnderHalvG.json")

        val oppdatertInntektResponse = lagInntektResponseFraJsonMedEnMåned(json)

        val forventetÅrligInntekt = 30000

        Assertions
            .assertThat(
                inntektsendringerService
                    .beregnEndretInntekt(
                        inntektResponse = oppdatertInntektResponse,
                        forventetInntektForPerson = forventetInntektForPerson(forventetÅrligInntekt),
                    ).harEndretInntekt(),
            ).isFalse
    }

    @Test
    fun `Ignorer utbetalinger av uførepensjon fra andre enn NAV`() {
        val json: String = readResource("inntekt/InntektUførepensjonFraAndreEnnFolketrygden.json")

        val oppdatertInntektResponse = lagInntektResponseFraJsonMedEnMåned(json)

        val forventetInntekt = 5000

        Assertions
            .assertThat(
                inntektsendringerService
                    .beregnEndretInntekt(
                        inntektResponse = oppdatertInntektResponse,
                        forventetInntektForPerson = ForventetInntektForPerson(personIdent = "1", forventetInntektForrigeMåned = forventetInntekt, forventetInntektToMånederTilbake = forventetInntekt, forventetInntektTreMånederTilbake = forventetInntekt, forventetInntektFireMånederTilbake = forventetInntekt),
                    ).harEndretInntekt(),
            ).isFalse
    }

    @Test
    fun `Ferieutbetalinger skal medberegnes`() {
        val json: String = readResource("inntekt/InntektFeriepengerSkalMedberegnes.json")

        val oppdatertInntektResponse = lagInntektResponseFraJsonMedEnMåned(json)

        Assertions
            .assertThat(
                inntektsendringerService
                    .beregnEndretInntekt(
                        inntektResponse = oppdatertInntektResponse,
                        forventetInntektForPerson = forventetInntektForPerson(forventetÅrligInntekt),
                    ).harEndretInntekt(),
            ).isTrue
    }

    fun inntektsendring(
        beløp: Int = 0,
        prosent: Int = 0,
        feilutbetaling: Int = 0,
    ): Inntektsendring {
        val beregningsResultat = beregningResultat(beløp, prosent, feilutbetaling)
        return Inntektsendring(beregningsResultat, beregningsResultat, beregningsResultat, beregningsResultat)
    }

    fun beregningResultat(
        beløp: Int = 0,
        prosent: Int = 0,
        feilutbetaling: Int = 0,
    ): BeregningResultat = BeregningResultat(beløp, prosent, feilutbetaling)

    private fun forventetInntektForPerson(forventetÅrligInntekt: Int) =
        ForventetInntektForPerson(
            personIdent = "1",
            forventetInntektForrigeMåned = forventetÅrligInntekt,
            forventetInntektToMånederTilbake = forventetÅrligInntekt,
            forventetInntektTreMånederTilbake = forventetÅrligInntekt,
            forventetInntektFireMånederTilbake = forventetÅrligInntekt,
        )
}
