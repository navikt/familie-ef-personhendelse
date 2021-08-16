package no.nav.familie.ef.personhendelse.client

import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.*


@Component
class SakClient(
    @Value("\${EF_SAK_URL}")
    private val uri: URI,
    @Value("\${EF_SAK_SCOPE}")
    private val scope: String,
    private val azureClient: AzureClient
) {

    fun finnesBehandlingForPerson(personIdent: String, stønadType: StønadType? = null): Boolean {
        val uriComponentsBuilder = UriComponentsBuilder.fromUri(uri)
            .pathSegment("api/ekstern/behandling/finnes")
        if (stønadType != null) {
            uriComponentsBuilder.queryParam("type", stønadType.name)
        }
        val token = azureClient.hentToken(scope)
        val headers = HttpHeaders()
        headers.setBearerAuth(token)
        headers.add("Nav-Call-Id", UUID.randomUUID().toString())
        headers.contentType = MediaType.APPLICATION_JSON

        val request = HttpEntity(PersonIdent(personIdent), headers)
        val response = RestTemplate().postForEntity<Ressurs<Boolean>>(uriComponentsBuilder.build().toUri(), request)
        return response.body?.data ?: error("Kall mot ef-sak feilet. Statuskode=${response.statusCode} - ${response.body?.melding}")
    }

}