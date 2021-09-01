package no.nav.familie.ef.personhendelse.client

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.personhendelse.client.pdl.PdlPersonRequest
import no.nav.familie.ef.personhendelse.client.pdl.PdlPersonRequestVariables
import no.nav.familie.ef.personhendelse.client.pdl.PdlResponse
import no.nav.familie.ef.personhendelse.client.pdl.feilsjekkOgReturnerData
import no.nav.familie.ef.personhendelse.generated.HentPerson
import no.nav.familie.ef.personhendelse.generated.hentperson.Person
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.objectMapper
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ResourceLoader
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class PdlClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${PDL_URL}")
    val url: URI
) : AbstractRestClient(restOperations, "pdl") {

    val pdlUri: URI = UriComponentsBuilder.fromUri(url).build().toUri()

    val hentPersonQuery = javaClass.getResource("/pdl/queries/hentPerson.graphql").readText().graphqlCompatible()

    fun hentPerson(fnr: String): Person {

        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(fnr),
            query = hentPersonQuery
        )

        val pdlResponse: String = postForEntity(pdlUri, pdlPersonRequest, httpHeadersPdl())
        secureLogger.info("pdlResponse: $pdlResponse")
        val mappedResponse: PdlResponse<HentPerson.Result> = objectMapper.readValue(pdlResponse)
        return feilsjekkOgReturnerData(fnr, mappedResponse) { it.hentPerson }
    }
}

private fun httpHeadersPdl(): HttpHeaders {

    return HttpHeaders().apply {
        add("Tema", "ENF")
    }
}

private fun String.graphqlCompatible(): String {
    return StringUtils.normalizeSpace(this.replace("\n", ""))
}