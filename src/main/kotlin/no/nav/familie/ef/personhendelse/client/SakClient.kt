package no.nav.familie.ef.personhendelse.client

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.*


@Component
class SakClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${EF_SAK_URL}")
    private val uri: URI
) : AbstractRestClient(restOperations, "familie.ef-sak") {

    fun finnesBehandlingForPerson(personIdent: String, stønadType: StønadType? = null): Boolean {
        val uriComponentsBuilder = UriComponentsBuilder.fromUri(uri)
            .pathSegment("api/ekstern/behandling/finnes")
        if (stønadType != null) {
            uriComponentsBuilder.queryParam("type", stønadType.name)
        }

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val request = HttpEntity(PersonIdent(personIdent), headers)
        val response = postForEntity<Ressurs<Boolean>>(uriComponentsBuilder.build().toUri(), request)
        return response.data ?: error("Kall mot ef-sak feilet. Status=${response.status} - ${response.melding}")
    }

}