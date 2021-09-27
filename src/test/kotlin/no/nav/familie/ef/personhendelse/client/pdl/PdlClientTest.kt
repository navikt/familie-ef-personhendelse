package no.nav.familie.ef.personhendelse.client.pdl

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ef.personhendelse.client.PdlClient
import no.nav.familie.ef.personhendelse.client.graphqlCompatible
import no.nav.familie.ef.personhendelse.generated.enums.Sivilstandstype
import no.nav.familie.ef.personhendelse.generated.scalars.Date
import no.nav.familie.ef.personhendelse.generated.scalars.DateTime
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
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(personIdent),
            query = hentPersonQuery
        )

        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlMatching("/graphql"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(pdlPersonRequest)))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(pdlResponse())
                ))

        val response = pdlClient.hentPerson(personIdent)
        Assertions.assertThat(response.statsborgerskap.first().land).isEqualTo("NOR")
        Assertions.assertThat(response.sivilstand.first().type).isEqualTo(Sivilstandstype.UGIFT)
        Assertions.assertThat(response.bostedsadresse.first().angittFlyttedato).isEqualTo(Date(LocalDate.of(1997,1,1)))
        Assertions.assertThat(response.bostedsadresse.first().gyldigFraOgMed).isEqualTo(DateTime(LocalDateTime.of(1997,1,1,0,0)))
        Assertions.assertThat(response.doedsfall.isEmpty()).isTrue
    }

    private fun pdlResponse(): String =
        "{\n" +
                "  \"data\": {\n" +
                "    \"hentPerson\": {\n" +
                "      \"forelderBarnRelasjon\": [],\n" +
                "      \"statsborgerskap\": [\n" +
                "        {\n" +
                "          \"land\": \"NOR\",\n" +
                "          \"gyldigFraOgMed\": \"1999-01-01\",\n" +
                "          \"gyldigTilOgMed\": null,\n" +
                "          \"metadata\": {\n" +
                "            \"historisk\": false\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"sivilstand\": [\n" +
                "        {\n" +
                "          \"type\": \"UGIFT\",\n" +
                "          \"gyldigFraOgMed\": null,\n" +
                "          \"bekreftelsesdato\": null,\n" +
                "          \"relatertVedSivilstand\": null\n" +
                "        }\n" +
                "      ],\n" +
                "      \"adressebeskyttelse\": [],\n" +
                "      \"bostedsadresse\": [\n" +
                "        {\n" +
                "          \"angittFlyttedato\": \"1997-01-01\",\n" +
                "          \"gyldigFraOgMed\": \"1997-01-01T00:00\",\n" +
                "          \"gyldigTilOgMed\": null,\n" +
                "          \"vegadresse\": {\n" +
                "            \"postnummer\": \"0557\"\n" +
                "          },\n" +
                "          \"matrikkeladresse\": null,\n" +
                "          \"ukjentBosted\": null,\n" +
                "          \"utenlandskAdresse\": null,\n" +
                "          \"metadata\": {\n" +
                "            \"historisk\": false\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"doedsfall\": [],\n" +
                "      \"foedsel\": [\n" +
                "        {\n" +
                "          \"foedselsdato\": \"1980-03-22\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}"
}