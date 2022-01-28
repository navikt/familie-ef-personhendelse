package no.nav.familie.ef.personhendelse.client

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class SakClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${EF_SAK_URL}")
    private val uri: URI
) : AbstractRestClient(restOperations, "familie.ef-sak") {

    fun harStønadSiste12MånederForPersonidenter(personidenter: Set<String>): Boolean {
        val uriComponentsBuilder = UriComponentsBuilder.fromUri(uri)
            .pathSegment("api/ekstern/behandling/harstoenad/flere-identer")
        val response = postForEntity<Ressurs<Boolean>>(uriComponentsBuilder.build().toUri(), personidenter)
        return response.data ?: error("Kall mot ef-sak feilet. Status=${response.status} - ${response.melding}")
    }

    fun finnNyeBarnForBruker(personIdent: PersonIdent): List<String> {
        val uriComponentsBuilder = UriComponentsBuilder.fromUri(uri)
            .pathSegment("/api/behandling/barn/nye-barn")
        val response = postForEntity<Ressurs<List<String>>>(uriComponentsBuilder.build().toUri(), personIdent)
        return response.getDataOrThrow()
    }
}
