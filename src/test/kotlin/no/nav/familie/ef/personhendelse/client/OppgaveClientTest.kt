package no.nav.familie.ef.personhendelse.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.io.IOException
import java.net.URI

class OppgaveClientTest {

    companion object {

        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var oppgaveClient: OppgaveClient
        lateinit var wiremockServerItem: WireMockServer

        private val personIdent = "12345678910"

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServerItem.start()
            oppgaveClient = OppgaveClient(URI.create(wiremockServerItem.baseUrl()).toString(), restOperations)
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
    fun `Opprett oppgave og returnert oppgaveId`() {
        wiremockServerItem.stubFor(
            post(urlMatching("/api/oppgave/opprett"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(gyldigResponse()),
                ),
        )

        val request = defaultOpprettOppgaveRequest(personIdent, "test")
        val response = oppgaveClient.opprettOppgave(request)
        Assertions.assertThat(response).isEqualTo(123)
    }

    @Throws(IOException::class)
    private fun gyldigResponse(): String {
        return "{\n" +
            "    \"data\": {\n" +
            "       \"oppgaveId\": \"123\"\n" +
            "},\n" +
            "    \"status\": \"SUKSESS\",\n" +
            "    \"melding\": \"Innhenting av data var vellykket\",\n" +
            "    \"stacktrace\": null\n" +
            "}"
    }

    fun defaultOpprettOppgaveRequest(personIdent: String, beskrivelse: String) =
        OpprettOppgaveRequest(
            ident = OppgaveIdentV2(ident = personIdent, gruppe = IdentGruppe.FOLKEREGISTERIDENT),
            saksId = null,
            tema = Tema.ENF,
            oppgavetype = Oppgavetype.VurderLivshendelse,
            fristFerdigstillelse = fristFerdigstillelse(),
            beskrivelse = beskrivelse,
            enhetsnummer = null,
            behandlingstema = Behandlingstema.Overgangsstønad.value,
            tilordnetRessurs = null,
            behandlesAvApplikasjon = null,
        )
}
