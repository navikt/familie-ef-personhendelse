package no.nav.familie.ef.personhendelse.client

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

@Component
class OppgaveClient(
    @Value("\${FAMILIE_INTEGRASJONER_API_URL}") val integrasjonUrl: String,
    @Qualifier("azure") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "familie.integrasjoner") {

    val oppgaveUrl = "$integrasjonUrl/api/oppgave"

    fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest): Long? {

        val opprettOppgaveUri = URI.create("$oppgaveUrl/opprett")
        val response =
            postForEntity<Ressurs<OppgaveResponse>>(
                opprettOppgaveUri,
                opprettOppgaveRequest,
                HttpHeaders().medContentTypeJsonUTF8()
            )
        return response.data?.oppgaveId
    }

}

fun defaultOpprettOppgaveRequest(personIdent: String, opplysningstypeBeskrivelse: String) =
    OpprettOppgaveRequest(
        ident = OppgaveIdentV2(ident = personIdent, gruppe = IdentGruppe.FOLKEREGISTERIDENT),
        saksId = null,
        tema = Tema.ENF,
        oppgavetype = Oppgavetype.VurderLivshendelse,
        fristFerdigstillelse = LocalDate.now(),
        beskrivelse = "Opprettet som følge av personhendelse av type $opplysningstypeBeskrivelse",
        enhetsnummer = null,
        behandlingstema = Behandlingstema.Overgangsstønad.value,
        tilordnetRessurs = null,
        behandlesAvApplikasjon = "familie-ef-sak"
    )

fun HttpHeaders.medContentTypeJsonUTF8(): HttpHeaders {
    this.add("Content-Type", "application/json;charset=UTF-8")
    this.acceptCharset = listOf(Charsets.UTF_8)
    return this
}