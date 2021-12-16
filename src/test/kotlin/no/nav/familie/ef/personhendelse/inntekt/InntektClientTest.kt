package no.nav.familie.ef.personhendelse.inntekt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import kotlinx.coroutines.runBlocking
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.YearMonth

class InntektClientTest {

    companion object {

        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var inntektClient: InntektClient
        lateinit var wiremockServerItem: WireMockServer

        private val personIdent = "12345678910"

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServerItem.start()
            inntektClient = InntektClient(URI.create(wiremockServerItem.baseUrl()), restOperations)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wiremockServerItem.stop()
        }
    }

    @AfterEach
    fun tearDownEachTest() {
        wiremockServerItem.resetAll()
    }

    @Test
    fun `tester inntektsresponse`() {

        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlEqualTo("/api/inntekt?fom=2020-11&tom=2021-11"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(PersonIdent(personIdent))))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(gyldigResponse)
                )
        )

        val response = runBlocking { inntektClient.hentInntekt(personIdent, YearMonth.of(2020, 11), YearMonth.of(2021, 11)) }

        val inntektType = response.arbeidsinntektMåned?.first()?.arbeidsInntektInformasjon?.inntektListe?.first()?.inntektType
        val beløp = response.arbeidsinntektMåned?.first()?.arbeidsInntektInformasjon?.inntektListe?.first()?.beløp
        Assertions.assertEquals(InntektType.LOENNSINNTEKT, inntektType)
        Assertions.assertEquals(40000, beløp)
    }

    private val gyldigResponse = """
        {
            "arbeidsInntektMaaned": [
                {
                    "aarMaaned": "2021-08",
                    "arbeidsInntektInformasjon": {
                        "inntektListe": [
                            {
                                "inntektType": "LOENNSINNTEKT",
                                "beloep": 40000,
                                "fordel": "kontantytelse",
                                "inntektskilde": "A-ordningen",
                                "inntektsperiodetype": "Maaned",
                                "inntektsstatus": "LoependeInnrapportert",
                                "leveringstidspunkt": "2021-12",
                                "opptjeningsland": "NO",
                                "skattemessigBosattLand": "NO",
                                "utbetaltIMaaned": "2021-08",
                                "opplysningspliktig": {
                                    "identifikator": "928497704",
                                    "aktoerType": "ORGANISASJON"
                                },
                                "virksomhet": {
                                    "identifikator": "947064649",
                                    "aktoerType": "ORGANISASJON"
                                },
                                "tilleggsinformasjon": {
                                    "kategori": "NorskKontinentalsokkel"
                                },
                                "inntektsmottaker": {
                                    "identifikator": "03127725224",
                                    "aktoerType": "NATURLIG_IDENT"
                                },
                                "inngaarIGrunnlagForTrekk": true,
                                "utloeserArbeidsgiveravgift": true,
                                "informasjonsstatus": "InngaarAlltid",
                                "beskrivelse": "fastloenn"
                            }
                        ]
                    }
                }
            ],
            "ident": {
                "identifikator": "03127725224",
                "aktoerType": "NATURLIG_IDENT"
            }
        }
    """.trimIndent()
}
