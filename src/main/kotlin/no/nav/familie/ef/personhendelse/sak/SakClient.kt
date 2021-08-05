package no.nav.familie.ef.personhendelse.sak

import no.nav.familie.ef.personhendelse.common.AzureClient
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.*


@Component
class SakClient(
    @Value("\${EF_SAK_URL}")
    private val uri: URI,
    private val azureClient: AzureClient
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun finnesBehandlingForPerson(personIdent: String, stønadType: StønadType? = null): Boolean {
        val uriComponentsBuilder = UriComponentsBuilder.fromUri(uri)
            .pathSegment("api/ekstern/behandling/finnes")
        if (stønadType != null) {
            uriComponentsBuilder.queryParam("type", stønadType.name)
        }
        logger.info("henter token")
        val token = azureClient.hentToken()
        logger.info("Antall chars for uthentet token fra azure: ${token.length}" )
        val headers = HttpHeaders()
        headers.setBearerAuth(token)
        headers.add("Nav-Call-Id", UUID.randomUUID().toString())
        headers.contentType = MediaType.APPLICATION_JSON

        val request = HttpEntity(PersonIdent(personIdent), headers)
        val response = RestTemplate().postForEntity<Ressurs<Boolean>>(uriComponentsBuilder.build().toUri(), request)
        logger.info("response kode ${response.statusCode} for finnesBehandling request mot ef-sak")
        return response.body?.data ?: error("Kall mot ef-sak feilet. Statuskode=${response.statusCode} - ${response.body?.melding}")
    }

}