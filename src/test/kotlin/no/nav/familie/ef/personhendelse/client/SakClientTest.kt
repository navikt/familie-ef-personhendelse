package no.nav.familie.ef.personhendelse.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

internal class SakClientTest {

    companion object {

        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var sakClient: SakClient
        lateinit var wiremockServerItem: WireMockServer

        private val personidenter = setOf("12345678910")

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServerItem.start()
            sakClient = SakClient(restOperations, URI.create(wiremockServerItem.baseUrl()))
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
    fun `Finnes behandling i ef-sak for person og returner true`() {

        wiremockServerItem.stubFor(
            post(urlMatching("/api/ekstern/behandling/har-loepende-stoenad"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(personidenter)))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(harStoenadGyldigResponse)
                )
        )

        val response = sakClient.harLøpendeStønad(personidenter)
        Assertions.assertThat(response).isEqualTo(true)
    }

    @Test
    fun `Finnes behandling med gitt eksternId i ef-sak for person med forventet inntekt på 400 000 for gitt dato`() {

        wiremockServerItem.stubFor(
            get(urlEqualTo("/api/vedtak/eksternid/1/inntekt?dato=${LocalDate.now()}"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(inntektGyldigResponse)
                )
        )

        val response = sakClient.inntektForEksternId(1)
        Assertions.assertThat(response).isEqualTo(400000)
    }

    @Test
    fun `Les gyldig response fra gjeldendeIverksatteBehandlingerMedInntekt`() {
        wiremockServerItem.stubFor(
            post(urlEqualTo("/api/vedtak/gjeldendeIverksatteBehandlingerMedInntekt"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(gjeldendeIverksatteBehandlingerGyldigResponse)
                )
        )
        val response = sakClient.hentForventetInntektForIdenter(listOf("1", "2"))
        Assertions.assertThat(response.first().forventetInntektForrigeMåned).isEqualTo(100_000)
    }

    private val harStoenadGyldigResponse = """
        {
            "data": true,
            "status": "SUKSESS",
            "melding": "Innhenting av data var vellykket"
        }
    """.trimIndent()

    private val inntektGyldigResponse = """
        {
            "data": 400000,
            "status": "SUKSESS",
            "melding": "Innhenting av data var vellykket"
        }
    """.trimIndent()

    private val gjeldendeIverksatteBehandlingerGyldigResponse = """
        {
            "data": [
                {
                    "personIdent": "1",
                    "forventetInntektForrigeMåned": 100000,
                    "forventetInntektToMånederTilbake": 200000
                },
                {
                    "personIdent": "2",
                    "forventetInntektForrigeMåned": 400000,
                    "forventetInntektToMånederTilbake": 350000
                }
            ],
            "status": "SUKSESS",
            "melding": "Innhenting av data var vellykket",
            "frontendFeilmelding": null,
            "stacktrace": null
        }
    """.trimIndent()
}
