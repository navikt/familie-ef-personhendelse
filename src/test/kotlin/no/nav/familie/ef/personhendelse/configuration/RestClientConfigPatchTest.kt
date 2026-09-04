package no.nav.familie.ef.personhendelse.configuration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.felles.tokenklient.entraid.EntraIDRestClientFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import java.net.URI
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible

class RestClientConfigPatchTest {
    data class PatchDto(
        val mappeId: Long,
        val beskrivelse: String,
    )

    @AfterEach
    fun tearDownEachTest() {
        wiremockServer.resetAll()
    }

    @TestFactory
    fun `alle RestClient-bønner i RestClientConfig støtter PATCH med body`(): List<DynamicTest> {
        val restClientBeanFunksjoner =
            RestClientConfig::class
                .memberFunctions
                .filter { it.returnType.classifier == RestClient::class }

        assertThat(restClientBeanFunksjoner).isNotEmpty

        return restClientBeanFunksjoner.map { funksjon ->
            DynamicTest.dynamicTest(funksjon.name) {
                funksjon.isAccessible = true
                val args =
                    funksjon.parameters
                        .drop(1) // dropp "this"-parameteret
                        .associateWith { "dummy-scope" }
                val restClient = funksjon.callBy(mapOf(funksjon.parameters[0] to restClientConfig) + args) as RestClient

                val path = "/test-patch-${funksjon.name}"
                wiremockServer.stubFor(
                    patch(urlEqualTo(path))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{}"),
                        ),
                )

                restClient
                    .patch()
                    .uri(URI.create("${wiremockServer.baseUrl()}$path"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(PatchDto(mappeId = 1L, beskrivelse = "test"))
                    .retrieve()
                    .toBodilessEntity()

                val body =
                    wiremockServer
                        .findAll(patchRequestedFor(urlEqualTo(path)))
                        .single()
                        .bodyAsString
                assertThat(body)
                    .withFailMessage(
                        "RestClient-bønnen '${funksjon.name}' klarte ikke å sende PATCH med body. " +
                            "Sjekk at requestFactory støtter HTTP PATCH (f.eks. JdkClientHttpRequestFactory, " +
                            "ikke SimpleClientHttpRequestFactory som kaster 'Invalid HTTP method: PATCH').",
                    ).contains("\"mappeId\":1")
            }
        }
    }

    companion object {
        private lateinit var wiremockServer: WireMockServer
        private lateinit var restClientConfig: RestClientConfig

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServer.start()

            val entraIDRestClientFactory =
                mockk<EntraIDRestClientFactory> {
                    every { lagMaskinTilMaskinRestKlient(any()) } answers { RestClient.builder().build() }
                    every { lagHybridRestKlient(any(), any()) } answers { RestClient.builder().build() }
                }

            restClientConfig = RestClientConfig(entraIDRestClientFactory)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wiremockServer.stop()
        }
    }
}
