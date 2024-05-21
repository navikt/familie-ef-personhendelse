package no.nav.familie.ef.personhendelse.client.pdl

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ef.personhendelse.generated.enums.Sivilstandstype
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

class PdlClientTest {
    companion object {
        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var pdlClient: PdlClient
        lateinit var wiremockServerItem: WireMockServer

        private val personIdent = "12345678910"

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServerItem.start()
            pdlClient = PdlClient(restOperations, URI.create(wiremockServerItem.baseUrl() + "/graphql"))
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
    fun `HentPerson query skal returnere person`() {
        val hentPersonQuery = javaClass.getResource("/pdl/queries/hentPerson.graphql").readText().graphqlCompatible()
        val pdlPersonRequest =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(personIdent),
                query = hentPersonQuery,
            )

        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlMatching("/graphql"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(pdlPersonRequest)))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pdlResponse()),
                ),
        )

        val response = pdlClient.hentPerson(personIdent)
        assertThat(response.statsborgerskap.first().land).isEqualTo("NOR")
        assertThat(response.sivilstand.first().type).isEqualTo(Sivilstandstype.UGIFT)
        assertThat(response.bostedsadresse.first().angittFlyttedato).isEqualTo(LocalDate.of(1997, 1, 1))
        assertThat(response.bostedsadresse.first().gyldigFraOgMed).isEqualTo(LocalDateTime.of(1997, 1, 1, 0, 0))
        assertThat(response.doedsfall.isEmpty()).isTrue
    }

    private fun pdlResponse(): String =
        """
        {
          "data": {
            "hentPerson": {
              "forelderBarnRelasjon": [],
              "statsborgerskap": [
                {
                  "land": "NOR",
                  "gyldigFraOgMed": "1999-01-01",
                  "gyldigTilOgMed": null,
                  "metadata": {
                    "historisk": false
                  }
                }
              ],
              "sivilstand": [
                {
                  "type": "UGIFT",
                  "gyldigFraOgMed": null,
                  "bekreftelsesdato": null,
                  "relatertVedSivilstand": null
                }
              ],
              "adressebeskyttelse": [],
              "bostedsadresse": [
                {
                  "angittFlyttedato": "1997-01-01",
                  "gyldigFraOgMed": "1997-01-01T00:00",
                  "gyldigTilOgMed": null,
                  "vegadresse": {
                    "postnummer": "0557"
                  },
                  "matrikkeladresse": null,
                  "ukjentBosted": null,
                  "utenlandskAdresse": null,
                  "metadata": {
                    "historisk": false
                  }
                }
              ],
              "doedsfall": [],
              "foedsel": [
                {
                  "foedselsdato": "1980-03-22"
                }
              ]
            }
          }
        }
        """.trimIndent()
}
