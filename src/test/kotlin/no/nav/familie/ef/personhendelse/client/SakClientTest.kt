package no.nav.familie.ef.personhendelse.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.io.IOException
import java.net.URI


internal class SakClientTest {

    companion object {

        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var sakClient: SakClient
        lateinit var wiremockServerItem: WireMockServer

        private val personIdent = "12345678910"

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
    fun `hentSaksnummer skal returnere fagsakId`() {

        wiremockServerItem.stubFor(
            post(urlMatching("/api/ekstern/behandling/finnes"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(PersonIdent(personIdent))))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(gyldigResponse())
                ))

        val response = sakClient.finnesBehandlingForPerson(personIdent)
        Assertions.assertThat(response).isEqualTo(true)
    }

    @Throws(IOException::class)
    private fun gyldigResponse(): String {
        return "{\n" +
                "    \"data\": true,\n" +
                "    \"status\": \"SUKSESS\",\n" +
                "    \"melding\": \"Innhenting av data var vellykket\",\n" +
                "    \"stacktrace\": null\n" +
                "}"
    }
}