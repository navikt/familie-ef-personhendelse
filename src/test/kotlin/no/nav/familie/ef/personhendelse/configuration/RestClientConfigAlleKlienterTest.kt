package no.nav.familie.ef.personhendelse.configuration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
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
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.YearMonth
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible

/**
 * Regresjonstest som fanger opp at *alle* RestClient-bønner definert i RestClientConfig faktisk
 * serialiserer felter som starter med æ/ø/å riktig - ikke bare den vi manuelt har testet
 * (integrasjonerRestClient). Testen finner alle @Bean-metoder i RestClientConfig som returnerer
 * RestClient via refleksjon, slik at den også dekker nye RestClient-bønner som legges til i
 * fremtiden uten at noen husker å legge til en egen test for dem.
 *
 * Se RestClientConfigJsonMapperTest for detaljert forklaring av selve bugen.
 */
class RestClientConfigAlleKlienterTest {
    data class DtoMedÆøåFelt(
        val årMånedFra: YearMonth,
        val vanligFelt: String,
    )

    @AfterEach
    fun tearDownEachTest() {
        wiremockServer.resetAll()
    }

    @TestFactory
    fun `alle RestClient-bønner i RestClientConfig serialiserer felter som starter med æøå riktig`(): List<DynamicTest> {
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

                val path = "/test-${funksjon.name}"
                wiremockServer.stubFor(
                    post(urlEqualTo(path))
                        .willReturn(
                            aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{}"),
                        ),
                )

                restClient
                    .post()
                    .uri(URI.create("${wiremockServer.baseUrl()}$path"))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(DtoMedÆøåFelt(YearMonth.of(2024, 1), "verdi"))
                    .retrieve()
                    .toBodilessEntity()

                val body =
                    wiremockServer
                        .findAll(postRequestedFor(urlEqualTo(path)))
                        .single()
                        .bodyAsString
                assertThat(body)
                    .withFailMessage(
                        "RestClient-bønnen '${funksjon.name}' mister felt som starter med æ/ø/å ved " +
                            "serialisering. Sjekk at bønnen bruker medTimeoutOgJsonMapper() i RestClientConfig.",
                    ).contains("\"årMånedFra\":\"2024-01\"")
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
                    // Speiler den ekte fabrikken sin oppførsel: bygger RestClient via
                    // RestClient.builder() direkte, uten den riktige jsonMapper-en.
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
