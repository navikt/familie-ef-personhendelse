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
                        .withBody(gyldigResponse),
                ),
        )

        val response = runBlocking { inntektClient.hentInntekt(personIdent, YearMonth.of(2020, 11), YearMonth.of(2021, 11)) }

        val inntektType = response.arbeidsinntektMåned?.first()?.arbeidsInntektInformasjon?.inntektListe?.first()?.inntektType
        val beløp = response.arbeidsinntektMåned?.first()?.arbeidsInntektInformasjon?.inntektListe?.first()?.beløp
        Assertions.assertEquals(InntektType.LOENNSINNTEKT, inntektType)
        Assertions.assertEquals(40000, beløp)
    }

    @Test
    fun `tester inntektshistorikk response`() {
        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlEqualTo("/api/inntekt/historikk?fom=2020-01&tom=2020-07"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(PersonIdent(personIdent))))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(gyldigInntekthistorikkResponse),
                ),
        )

        val response = runBlocking { inntektClient.hentInntektshistorikk(personIdent, YearMonth.of(2020, 1), YearMonth.of(2020, 7)) }

        val inntektType = response.aarMaanedHistorikk.values.first().values.first().first().arbeidsInntektInformasjon.inntektListe?.first()?.inntektType
        val beløp = response.aarMaanedHistorikk.values.first().values.first().first().arbeidsInntektInformasjon.inntektListe?.first()?.beløp
        val inntektsversjon = response.aarMaanedHistorikk.values.first().values.first().first().versjon

        Assertions.assertEquals(InntektType.LOENNSINNTEKT, inntektType)
        Assertions.assertEquals(35000, beløp)
        Assertions.assertEquals(2, inntektsversjon)
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
                                    "identifikator": "123456789",
                                    "aktoerType": "ORGANISASJON"
                                },
                                "virksomhet": {
                                    "identifikator": "123456788",
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

    private val gyldigInntekthistorikkResponse = """
        {
            "aarMaanedHistorikk": {
                "2020-01": {
                    "123456789": [
                        {
                            "versjon": 2,
                            "opplysningspliktig": "123456789",
                            "virksomhet": "123456788",
                            "innleveringstidspunkt": "2021-12-23T11:40:27.177",
                            "arbeidsInntektInformasjon": {
                                "inntektListe": [
                                    {
                                        "inntektType": "LOENNSINNTEKT",
                                        "beloep": 35000,
                                        "fordel": "kontantytelse",
                                        "inntektskilde": "A-ordningen",
                                        "inntektsperiodetype": "Maaned",
                                        "inntektsstatus": "LoependeInnrapportert",
                                        "leveringstidspunkt": "2022-01",
                                        "opptjeningsland": "NO",
                                        "utbetaltIMaaned": "2020-01",
                                        "opplysningspliktig": {
                                            "identifikator": "123456789",
                                            "aktoerType": "ORGANISASJON"
                                        },
                                        "virksomhet": {
                                            "identifikator": "123456788",
                                            "aktoerType": "ORGANISASJON"
                                        },
                                        "tilleggsinformasjon": {
                                            "kategori": "Nettoloennsordning"
                                        },
                                        "inntektsmottaker": {
                                            "identifikator": "07097614819",
                                            "aktoerType": "NATURLIG_IDENT"
                                        },
                                        "inngaarIGrunnlagForTrekk": true,
                                        "utloeserArbeidsgiveravgift": true,
                                        "informasjonsstatus": "InngaarAlltid",
                                        "beskrivelse": "fastloenn",
                                        "skatteOgAvgiftsregel": "nettoloenn"
                                    }
                                ]
                            }
                        }
                    ]
                },
                "2020-02": {
                    "123456789": [
                        {
                            "versjon": 2,
                            "opplysningspliktig": "123456789",
                            "virksomhet": "123456788",
                            "innleveringstidspunkt": "2021-12-23T11:40:27.177",
                            "arbeidsInntektInformasjon": {
                                "inntektListe": [
                                    {
                                        "inntektType": "LOENNSINNTEKT",
                                        "beloep": 35000,
                                        "fordel": "kontantytelse",
                                        "inntektskilde": "A-ordningen",
                                        "inntektsperiodetype": "Maaned",
                                        "inntektsstatus": "LoependeInnrapportert",
                                        "leveringstidspunkt": "2022-01",
                                        "opptjeningsland": "NO",
                                        "utbetaltIMaaned": "2020-02",
                                        "opplysningspliktig": {
                                            "identifikator": "123456789",
                                            "aktoerType": "ORGANISASJON"
                                        },
                                        "virksomhet": {
                                            "identifikator": "123456788",
                                            "aktoerType": "ORGANISASJON"
                                        },
                                        "tilleggsinformasjon": {
                                            "kategori": "Nettoloennsordning"
                                        },
                                        "inntektsmottaker": {
                                            "identifikator": "07097614819",
                                            "aktoerType": "NATURLIG_IDENT"
                                        },
                                        "inngaarIGrunnlagForTrekk": true,
                                        "utloeserArbeidsgiveravgift": true,
                                        "informasjonsstatus": "InngaarAlltid",
                                        "beskrivelse": "fastloenn",
                                        "skatteOgAvgiftsregel": "nettoloenn"
                                    }
                                ]
                            }
                        }
                    ]
                },
                "2020-03": {
                    "123456789": [
                        {
                            "versjon": 2,
                            "opplysningspliktig": "123456789",
                            "virksomhet": "123456788",
                            "innleveringstidspunkt": "2021-12-23T11:40:27.177",
                            "arbeidsInntektInformasjon": {
                                "inntektListe": [
                                    {
                                        "inntektType": "LOENNSINNTEKT",
                                        "beloep": 35000,
                                        "fordel": "kontantytelse",
                                        "inntektskilde": "A-ordningen",
                                        "inntektsperiodetype": "Maaned",
                                        "inntektsstatus": "LoependeInnrapportert",
                                        "leveringstidspunkt": "2022-01",
                                        "opptjeningsland": "NO",
                                        "utbetaltIMaaned": "2020-03",
                                        "opplysningspliktig": {
                                            "identifikator": "123456789",
                                            "aktoerType": "ORGANISASJON"
                                        },
                                        "virksomhet": {
                                            "identifikator": "123456788",
                                            "aktoerType": "ORGANISASJON"
                                        },
                                        "tilleggsinformasjon": {
                                            "kategori": "Nettoloennsordning"
                                        },
                                        "inntektsmottaker": {
                                            "identifikator": "07097614819",
                                            "aktoerType": "NATURLIG_IDENT"
                                        },
                                        "inngaarIGrunnlagForTrekk": true,
                                        "utloeserArbeidsgiveravgift": true,
                                        "informasjonsstatus": "InngaarAlltid",
                                        "beskrivelse": "fastloenn",
                                        "skatteOgAvgiftsregel": "nettoloenn"
                                    }
                                ]
                            }
                        }
                    ]
                },
                "2020-04": {
                    "123456789": [
                        {
                            "versjon": 2,
                            "opplysningspliktig": "123456789",
                            "virksomhet": "123456788",
                            "innleveringstidspunkt": "2021-12-23T11:40:27.177",
                            "arbeidsInntektInformasjon": {
                                "inntektListe": [
                                    {
                                        "inntektType": "LOENNSINNTEKT",
                                        "beloep": 35000,
                                        "fordel": "kontantytelse",
                                        "inntektskilde": "A-ordningen",
                                        "inntektsperiodetype": "Maaned",
                                        "inntektsstatus": "LoependeInnrapportert",
                                        "leveringstidspunkt": "2022-01",
                                        "opptjeningsland": "NO",
                                        "utbetaltIMaaned": "2020-04",
                                        "opplysningspliktig": {
                                            "identifikator": "123456789",
                                            "aktoerType": "ORGANISASJON"
                                        },
                                        "virksomhet": {
                                            "identifikator": "123456788",
                                            "aktoerType": "ORGANISASJON"
                                        },
                                        "tilleggsinformasjon": {
                                            "kategori": "Nettoloennsordning"
                                        },
                                        "inntektsmottaker": {
                                            "identifikator": "07097614819",
                                            "aktoerType": "NATURLIG_IDENT"
                                        },
                                        "inngaarIGrunnlagForTrekk": true,
                                        "utloeserArbeidsgiveravgift": true,
                                        "informasjonsstatus": "InngaarAlltid",
                                        "beskrivelse": "fastloenn",
                                        "skatteOgAvgiftsregel": "nettoloenn"
                                    }
                                ]
                            }
                        }
                    ]
                },
                "2020-05": {
                    "123456789": [
                        {
                            "versjon": 2,
                            "opplysningspliktig": "123456789",
                            "virksomhet": "123456788",
                            "innleveringstidspunkt": "2021-12-23T11:40:27.177",
                            "arbeidsInntektInformasjon": {
                                "inntektListe": [
                                    {
                                        "inntektType": "LOENNSINNTEKT",
                                        "beloep": 35000,
                                        "fordel": "kontantytelse",
                                        "inntektskilde": "A-ordningen",
                                        "inntektsperiodetype": "Maaned",
                                        "inntektsstatus": "LoependeInnrapportert",
                                        "leveringstidspunkt": "2022-01",
                                        "opptjeningsland": "NO",
                                        "utbetaltIMaaned": "2020-05",
                                        "opplysningspliktig": {
                                            "identifikator": "123456789",
                                            "aktoerType": "ORGANISASJON"
                                        },
                                        "virksomhet": {
                                            "identifikator": "123456788",
                                            "aktoerType": "ORGANISASJON"
                                        },
                                        "tilleggsinformasjon": {
                                            "kategori": "Nettoloennsordning"
                                        },
                                        "inntektsmottaker": {
                                            "identifikator": "07097614819",
                                            "aktoerType": "NATURLIG_IDENT"
                                        },
                                        "inngaarIGrunnlagForTrekk": true,
                                        "utloeserArbeidsgiveravgift": true,
                                        "informasjonsstatus": "InngaarAlltid",
                                        "beskrivelse": "fastloenn",
                                        "skatteOgAvgiftsregel": "nettoloenn"
                                    }
                                ]
                            }
                        }
                    ]
                },
                "2020-06": {
                    "123456789": [
                        {
                            "versjon": 2,
                            "opplysningspliktig": "123456789",
                            "virksomhet": "123456788",
                            "innleveringstidspunkt": "2021-12-23T11:40:27.177",
                            "arbeidsInntektInformasjon": {
                                "inntektListe": [
                                    {
                                        "inntektType": "LOENNSINNTEKT",
                                        "beloep": 35000,
                                        "fordel": "kontantytelse",
                                        "inntektskilde": "A-ordningen",
                                        "inntektsperiodetype": "Maaned",
                                        "inntektsstatus": "LoependeInnrapportert",
                                        "leveringstidspunkt": "2022-01",
                                        "opptjeningsland": "NO",
                                        "utbetaltIMaaned": "2020-06",
                                        "opplysningspliktig": {
                                            "identifikator": "123456789",
                                            "aktoerType": "ORGANISASJON"
                                        },
                                        "virksomhet": {
                                            "identifikator": "123456788",
                                            "aktoerType": "ORGANISASJON"
                                        },
                                        "tilleggsinformasjon": {
                                            "kategori": "Nettoloennsordning"
                                        },
                                        "inntektsmottaker": {
                                            "identifikator": "07097614819",
                                            "aktoerType": "NATURLIG_IDENT"
                                        },
                                        "inngaarIGrunnlagForTrekk": true,
                                        "utloeserArbeidsgiveravgift": true,
                                        "informasjonsstatus": "InngaarAlltid",
                                        "beskrivelse": "fastloenn",
                                        "skatteOgAvgiftsregel": "nettoloenn"
                                    }
                                ]
                            }
                        }
                    ]
                },
                "2020-07": {
                    "123456789": [
                        {
                            "versjon": 2,
                            "opplysningspliktig": "123456789",
                            "virksomhet": "123456788",
                            "innleveringstidspunkt": "2021-12-23T11:40:27.177",
                            "arbeidsInntektInformasjon": {
                                "inntektListe": [
                                    {
                                        "inntektType": "LOENNSINNTEKT",
                                        "beloep": 35000,
                                        "fordel": "kontantytelse",
                                        "inntektskilde": "A-ordningen",
                                        "inntektsperiodetype": "Maaned",
                                        "inntektsstatus": "LoependeInnrapportert",
                                        "leveringstidspunkt": "2022-01",
                                        "opptjeningsland": "NO",
                                        "utbetaltIMaaned": "2020-07",
                                        "opplysningspliktig": {
                                            "identifikator": "123456789",
                                            "aktoerType": "ORGANISASJON"
                                        },
                                        "virksomhet": {
                                            "identifikator": "123456788",
                                            "aktoerType": "ORGANISASJON"
                                        },
                                        "tilleggsinformasjon": {
                                            "kategori": "Nettoloennsordning"
                                        },
                                        "inntektsmottaker": {
                                            "identifikator": "07097614819",
                                            "aktoerType": "NATURLIG_IDENT"
                                        },
                                        "inngaarIGrunnlagForTrekk": true,
                                        "utloeserArbeidsgiveravgift": true,
                                        "informasjonsstatus": "InngaarAlltid",
                                        "beskrivelse": "fastloenn",
                                        "skatteOgAvgiftsregel": "nettoloenn"
                                    }
                                ]
                            }
                        }
                    ]
                }
            }
        }
    """.trimIndent()
}
