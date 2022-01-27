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
            post(urlMatching("/api/ekstern/behandling/harstoenad/flere-identer"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(personidenter)))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(harStoenadGyldigResponse)
                )
        )

        val response = sakClient.harStønadSiste12MånederForPersonidenter(personidenter)
        Assertions.assertThat(response).isEqualTo(true)
    }

    @Test
    fun `Finnes behandling med gitt eksternId i ef-sak for person med forventet inntekt på 400 000 for gitt dato`() {

        wiremockServerItem.stubFor(
            get(urlEqualTo("/api/vedtak/eksternId/1?dato=${LocalDate.now()}"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(inntektGyldigResponse)
                )
        )

        val response = sakClient.inntektForEksternId(1)
        Assertions.assertThat(response).isEqualTo(400000)
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
}
