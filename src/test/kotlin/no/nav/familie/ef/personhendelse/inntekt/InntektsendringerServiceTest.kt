package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.personhendelse.client.ForventetInntektForPerson
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.YearMonth

class InntektsendringerServiceTest {

    private val oppgaveClient = mockk<OppgaveClient>()
    private val sakClient = mockk<SakClient>()

    val inntektsendringerService = InntektsendringerService(oppgaveClient, sakClient)
    val forventetLønnsinntekt = 420000 // 35k pr mnd i eksempel json-fil

    @BeforeEach
    internal fun setUp() {
        every { sakClient.inntektForEksternId(1) } returns forventetLønnsinntekt
        every { sakClient.inntektForEksternId(2) } returns (forventetLønnsinntekt * 0.9).toInt()
        every { sakClient.inntektForEksternId(3) } returns (forventetLønnsinntekt * 0.91).toInt()
    }

    @Test
    fun `Har endret inntekt med mer enn 10 prosent i forhold til forventet inntekt`() {
        val json: String = readResource("inntekt/InntekthistorikkLoennsinntektEksempel.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)

        val nyesteArbeidsInntektInformasjonIEksempelJson =
            inntektshistorikkResponse.inntektForMåned(YearMonth.of(2022, 1)).first().arbeidsInntektInformasjon
        val nestNyesteArbeidsInntektInformasjonIEksempelJson =
            inntektshistorikkResponse.inntektForMåned(YearMonth.of(2021, 12)).first().arbeidsInntektInformasjon

        val oppdatertDatoInntektshistorikkResponse = InntektshistorikkResponse(
            linkedMapOf(
                Pair(
                    YearMonth.now().minusMonths(1),
                    mapOf(
                        Pair(
                            "1",
                            listOf(
                                InntektVersjon(
                                    nyesteArbeidsInntektInformasjonIEksempelJson,
                                    null,
                                    "innleveringstidspunkt",
                                    "opplysningspliktig",
                                    1,
                                ),
                            ),
                        ),
                    ),
                ),
                Pair(
                    YearMonth.now().minusMonths(2),
                    mapOf(
                        Pair(
                            "1",
                            listOf(
                                InntektVersjon(
                                    nestNyesteArbeidsInntektInformasjonIEksempelJson,
                                    null,
                                    "innleveringstidspunkt",
                                    "opplysningspliktig",
                                    1,
                                ),
                            ),
                        ),
                    ),
                ),
                Pair(
                    YearMonth.now().minusMonths(3),
                    mapOf(
                        Pair(
                            "1",
                            listOf(
                                InntektVersjon(
                                    nestNyesteArbeidsInntektInformasjonIEksempelJson,
                                    null,
                                    "innleveringstidspunkt",
                                    "opplysningspliktig",
                                    1,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val forventetInntektTiProsentLavere = (forventetLønnsinntekt * 0.9).toInt()
        val forventetInntektNiProsentLavere = (forventetLønnsinntekt * 0.91).toInt()

        Assertions.assertThat(
            inntektsendringerService.beregnEndretInntekt(
                oppdatertDatoInntektshistorikkResponse,
                ForventetInntektForPerson("1", forventetLønnsinntekt, forventetLønnsinntekt, forventetLønnsinntekt),
            ),
        ).isEqualTo(Inntektsendring(0, 0, 0))
        Assertions.assertThat(
            inntektsendringerService.beregnEndretInntekt(
                oppdatertDatoInntektshistorikkResponse,
                ForventetInntektForPerson("2", forventetInntektTiProsentLavere, forventetInntektTiProsentLavere, forventetInntektTiProsentLavere),
            ),
        ).isEqualTo(Inntektsendring(11, 11, 11))
        Assertions.assertThat(
            inntektsendringerService.beregnEndretInntekt(
                oppdatertDatoInntektshistorikkResponse,
                ForventetInntektForPerson("3", forventetInntektNiProsentLavere, forventetInntektNiProsentLavere, forventetInntektNiProsentLavere),
            ),
        ).isEqualTo(Inntektsendring(9, 9, 9))
    }

    @Test
    fun `Utbetaling av offentlig ytelse og lønnsinntekt utgjør til sammen mer enn 10 prosent av forventet inntekt`() {
        val json: String = readResource("inntekt/InntekthistorikkLoennsinntektOgOffentligYtelseEksempel.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)

        val inntektInformasjonIEksempelJson =
            inntektshistorikkResponse.inntektForMåned(YearMonth.of(2021, 12)).first().arbeidsInntektInformasjon

        val oppdatertDatoInntektshistorikkResponse = InntektshistorikkResponse(
            linkedMapOf(
                Pair(
                    YearMonth.now().minusMonths(1),
                    mapOf(
                        Pair(
                            "5",
                            listOf(
                                InntektVersjon(
                                    inntektInformasjonIEksempelJson,
                                    null,
                                    "innleveringstidspunkt",
                                    "opplysningspliktig",
                                    1,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val forventetInntektTiProsentLavere = (forventetLønnsinntekt * 0.9).toInt()

        Assertions.assertThat(
            inntektsendringerService.beregnEndretInntekt(
                oppdatertDatoInntektshistorikkResponse,
                ForventetInntektForPerson("2", forventetInntektTiProsentLavere, forventetInntektTiProsentLavere, forventetInntektTiProsentLavere),
            ).harEndretInntekt(),
        ).isFalse
    }

    @Test
    fun `Bruker med mer enn 10 prosent inntektsendring pga etterbetaling skal ignoreres`() {
        val json: String = readResource("inntekt/InntekthistorikkEtterbetalingSkalIgnoreres.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)

        val inntektVersjonForNyesteMåned = inntektshistorikkResponse.inntektForMåned(YearMonth.of(2022, 2))

        val oppdatertDatoInntektshistorikkResponse = InntektshistorikkResponse(
            linkedMapOf(
                Pair(YearMonth.now().minusMonths(1), mapOf(Pair("1", inntektVersjonForNyesteMåned))),
            ),
        )
        val forventetInntekt = 172_000

        Assertions.assertThat(
            inntektsendringerService.beregnEndretInntekt(
                oppdatertDatoInntektshistorikkResponse,
                ForventetInntektForPerson("2", forventetInntekt, forventetInntekt, forventetInntekt),
            ).harEndretInntekt(),
        ).isFalse
    }

    @Test
    fun `Har for høy forventet inntekt, skal returnere false`() {
        val json: String = readResource("inntekt/InntekthistorikkLoennsinntektEksempel.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)

        val nyesteArbeidsInntektInformasjonIEksempelJson =
            inntektshistorikkResponse.inntektForMåned(YearMonth.of(2022, 1)).first().arbeidsInntektInformasjon
        val nestNyesteArbeidsInntektInformasjonIEksempelJson =
            inntektshistorikkResponse.inntektForMåned(YearMonth.of(2021, 12)).first().arbeidsInntektInformasjon

        val oppdatertDatoInntektshistorikkResponse = InntektshistorikkResponse(
            linkedMapOf(
                Pair(
                    YearMonth.now().minusMonths(1),
                    mapOf(
                        Pair(
                            "1",
                            listOf(
                                InntektVersjon(
                                    nyesteArbeidsInntektInformasjonIEksempelJson,
                                    null,
                                    "innleveringstidspunkt",
                                    "opplysningspliktig",
                                    1,
                                ),
                            ),
                        ),
                    ),
                ),
                Pair(
                    YearMonth.now().minusMonths(2),
                    mapOf(
                        Pair(
                            "1",
                            listOf(
                                InntektVersjon(
                                    nestNyesteArbeidsInntektInformasjonIEksempelJson,
                                    null,
                                    "innleveringstidspunkt",
                                    "opplysningspliktig",
                                    1,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val forHøyInntekt = 585001
        Assertions.assertThat(
            inntektsendringerService.beregnEndretInntekt(
                oppdatertDatoInntektshistorikkResponse,
                ForventetInntektForPerson("1", forHøyInntekt, forHøyInntekt, forHøyInntekt),
            ).harEndretInntekt(),
        ).isFalse
    }

    @Test
    fun `Har inntekt under halv G, skal returnere false selv om inntekt har økt mer enn 10 prosent`() {
        val json: String = readResource("inntekt/InntekthistorikkInntektUnderHalvG.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)

        val nyesteArbeidsInntektInformasjonIEksempelJson =
            inntektshistorikkResponse.inntektForMåned(YearMonth.of(2022, 1)).first().arbeidsInntektInformasjon
        val nestNyesteArbeidsInntektInformasjonIEksempelJson =
            inntektshistorikkResponse.inntektForMåned(YearMonth.of(2021, 12)).first().arbeidsInntektInformasjon

        val oppdatertDatoInntektshistorikkResponse = InntektshistorikkResponse(
            linkedMapOf(
                Pair(
                    YearMonth.now().minusMonths(1),
                    mapOf(
                        Pair(
                            "1",
                            listOf(
                                InntektVersjon(
                                    nyesteArbeidsInntektInformasjonIEksempelJson,
                                    null,
                                    "innleveringstidspunkt",
                                    "opplysningspliktig",
                                    1,
                                ),
                            ),
                        ),
                    ),
                ),
                Pair(
                    YearMonth.now().minusMonths(2),
                    mapOf(
                        Pair(
                            "1",
                            listOf(
                                InntektVersjon(
                                    nestNyesteArbeidsInntektInformasjonIEksempelJson,
                                    null,
                                    "innleveringstidspunkt",
                                    "opplysningspliktig",
                                    1,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val forventetInntekt = 30000
        Assertions.assertThat(
            inntektsendringerService.beregnEndretInntekt(
                oppdatertDatoInntektshistorikkResponse,
                ForventetInntektForPerson("1", forventetInntekt, forventetInntekt, forventetInntekt),
            ).harEndretInntekt(),
        ).isFalse
    }

    @Test
    fun `Ignorer utbetalinger av uførepensjon fra andre enn NAV`() {
        val json: String = readResource("inntekt/InntekthistorikkUførepensjonFraAndreEnnFolketrygden.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)

        val nyesteArbeidsInntektInformasjonIEksempelJson = inntektshistorikkResponse.inntektForMåned(YearMonth.of(2022, 1))

        val oppdatertDatoInntektshistorikkResponse = InntektshistorikkResponse(
            linkedMapOf(
                Pair(YearMonth.now().minusMonths(1), mapOf(Pair("1", nyesteArbeidsInntektInformasjonIEksempelJson))),
            ),
        )

        val forventetInntekt = 5000
        Assertions.assertThat(
            inntektsendringerService.beregnEndretInntekt(
                oppdatertDatoInntektshistorikkResponse,
                ForventetInntektForPerson("1", forventetInntekt, forventetInntekt, forventetInntekt),
            ).harEndretInntekt(),
        ).isFalse
    }

    @Test
    fun `Ignorer ferieutbetalinger`() {
        val json: String = readResource("inntekt/InntekthistorikkFeriepengerSkalIgnoreres.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)

        val nyesteArbeidsInntektInformasjonIEksempelJson =
            inntektshistorikkResponse.inntektForMåned(YearMonth.of(2022, 2)).first().arbeidsInntektInformasjon
        val nestNyesteArbeidsInntektInformasjonIEksempelJson =
            inntektshistorikkResponse.inntektForMåned(YearMonth.of(2022, 1)).first().arbeidsInntektInformasjon

        val oppdatertDatoInntektshistorikkResponse = InntektshistorikkResponse(
            linkedMapOf(
                Pair(
                    YearMonth.now().minusMonths(1),
                    mapOf(
                        Pair(
                            "1",
                            listOf(
                                InntektVersjon(
                                    nyesteArbeidsInntektInformasjonIEksempelJson,
                                    null,
                                    "innleveringstidspunkt",
                                    "opplysningspliktig",
                                    1,
                                ),
                            ),
                        ),
                    ),
                ),
                Pair(
                    YearMonth.now().minusMonths(2),
                    mapOf(
                        Pair(
                            "1",
                            listOf(
                                InntektVersjon(
                                    nestNyesteArbeidsInntektInformasjonIEksempelJson,
                                    null,
                                    "innleveringstidspunkt",
                                    "opplysningspliktig",
                                    1,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        Assertions.assertThat(
            inntektsendringerService.beregnEndretInntekt(
                oppdatertDatoInntektshistorikkResponse,
                ForventetInntektForPerson("3", forventetLønnsinntekt, forventetLønnsinntekt, forventetLønnsinntekt),
            ).harEndretInntekt(),
        ).isFalse
    }

    @Test
    fun `Inntekt som frilanser skal medberegnes`() {
        val json: String = readResource("inntekt/InntekthistorikkFrilanser.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)

        val nyesteArbeidsInntektInformasjonIEksempelJson = inntektshistorikkResponse.inntektForMåned(YearMonth.of(2022, 3))
        val nestNyesteArbeidsInntektInformasjonIEksempelJson = inntektshistorikkResponse.inntektForMåned(YearMonth.of(2022, 3))
        val treMånederTilbakeArbeidsInntektInformasjonIEksempelJson = inntektshistorikkResponse.inntektForMåned(YearMonth.of(2022, 3))

        val oppdatertDatoInntektshistorikkResponse = InntektshistorikkResponse(
            linkedMapOf(
                Pair(YearMonth.now().minusMonths(1), mapOf(Pair("1", nyesteArbeidsInntektInformasjonIEksempelJson))),
                Pair(
                    YearMonth.now().minusMonths(2),
                    mapOf(Pair("1", nestNyesteArbeidsInntektInformasjonIEksempelJson)),
                ),
                Pair(
                    YearMonth.now().minusMonths(3),
                    mapOf(Pair("1", treMånederTilbakeArbeidsInntektInformasjonIEksempelJson)),
                ),
            ),
        )

        val forventetInntekt = 70000
        Assertions.assertThat(
            inntektsendringerService.beregnEndretInntekt(
                oppdatertDatoInntektshistorikkResponse,
                ForventetInntektForPerson("3", forventetInntekt, forventetInntekt, forventetInntekt),
            ).harEndretInntekt(),
        ).isTrue
    }

    fun readResource(name: String): String {
        return this::class.java.classLoader.getResource(name)!!.readText(StandardCharsets.UTF_8)
    }
}
