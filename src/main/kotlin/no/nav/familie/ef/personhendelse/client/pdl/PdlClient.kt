package no.nav.familie.ef.personhendelse.client

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import kotlinx.coroutines.runBlocking
import no.nav.familie.ef.personhendelse.client.pdl.PdlPersonRequest
import no.nav.familie.ef.personhendelse.client.pdl.PdlPersonRequestVariables
import no.nav.familie.ef.personhendelse.client.pdl.PdlResponse
import no.nav.familie.ef.personhendelse.client.pdl.feilsjekkOgReturnerData
import no.nav.familie.ef.personhendelse.generated.HentPerson
import no.nav.familie.ef.personhendelse.generated.hentperson.Person
import no.nav.familie.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class PdlClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${PDL_URL}")
    val url: URI
) : AbstractRestClient(restOperations, "pdl") {

    val pathGraphql = "graphql"
    val pdlUri: URI = UriComponentsBuilder.fromUri(url).pathSegment(pathGraphql).build().toUri()

    fun hentPerson(fnr: String): Person {

        val variables = HentPerson.Variables(fnr, true, true)
        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(fnr),
            query = HentPerson(variables).query
        )

        val pdlResponse: PdlResponse<HentPerson.Result> = postForEntity(pdlUri, pdlPersonRequest, httpHeadersPdl())
        return feilsjekkOgReturnerData(fnr, pdlResponse) { it.hentPerson }
    }
}

private fun httpHeadersPdl(): HttpHeaders {

    return HttpHeaders().apply {
        add("Tema", "ENF")
    }
}