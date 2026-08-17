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
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.YearMonth

/**
 * Regresjonstest for feilen der felter som starter med æ/ø/å (f.eks. årMånedFra) forsvinner fra
 * JSON-en når en RestClient bygges med RestClient.builder() direkte (slik EntraIDRestClientFactory
 * sine fabrikkmetoder gjør), uten at jsonMapper fra no.nav.familie.kontrakter.felles blir registrert.
 * Spring sin default JsonMapper mangler KotlinFeature.KotlinPropertyNameAsImplicitName, og da mister
 * Kotlin-modulen navnet på felter som starter med æ/ø/å helt (feltet blir ikke bare feilaktig
 * navngitt, men utelatt fra JSON-en).
 */
class RestClientConfigJsonMapperTest {
    data class DtoMedÆøåFelt(
        val årMånedFra: YearMonth,
        val vanligFelt: String,
    )

    @AfterEach
    fun tearDownEachTest() {
        wiremockServer.resetAll()
    }

    @Test
    fun `integrasjonerRestClient serialiserer felter som starter med æøå riktig`() {
        wiremockServer.stubFor(
            post(urlEqualTo("/test"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody("{}"),
                ),
        )

        val restClient = restClientConfig.integrasjonerRestClient("dummy-scope")
        restClient
            .post()
            .uri(URI.create("${wiremockServer.baseUrl()}/test"))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(DtoMedÆøåFelt(YearMonth.of(2024, 1), "verdi"))
            .retrieve()
            .toBodilessEntity()

        val body =
            wiremockServer
                .findAll(postRequestedFor(urlEqualTo("/test")))
                .single()
                .bodyAsString
        assertThat(body).contains("\"årMånedFra\":\"2024-01\"")
    }

    @Test
    fun `default RestClient uten fix mister felt som starter med æøå (dokumenterer bugen)`() {
        wiremockServer.stubFor(
            post(urlEqualTo("/test-uten-fix"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody("{}"),
                ),
        )

        val restClientUtenFix = RestClient.builder().build()
        restClientUtenFix
            .post()
            .uri(URI.create("${wiremockServer.baseUrl()}/test-uten-fix"))
            .body(DtoMedÆøåFelt(YearMonth.of(2024, 1), "verdi"))
            .retrieve()
            .toBodilessEntity()

        val body =
            wiremockServer
                .findAll(postRequestedFor(urlEqualTo("/test-uten-fix")))
                .single()
                .bodyAsString
        assertThat(body).doesNotContain("årMånedFra")
        assertThat(body).contains("vanligFelt")
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
