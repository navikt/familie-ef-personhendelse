package no.nav.familie.ef.personhendelse.pdl

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import kotlinx.coroutines.runBlocking
import no.nav.familie.ef.personhendelse.common.AzureClient
import no.nav.familie.ef.personhendelse.generated.HentPerson
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class PdlClient(
    val azureClient: AzureClient,
    @Value("\${PDL_URL}")
    val url: String,
    @Value("\${PDL_SCOPE}")
    val scope: String
) {

    fun hentPerson(fnr: String, callId: String): GraphQLClientResponse<HentPerson.Result> {

        val variables = HentPerson.Variables(fnr, true, true)
        val hentPersonQuery = HentPerson(variables)

        val client = GraphQLWebClient(
            url = url,
            builder = WebClient.builder().defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${azureClient.hentToken(scope)}")
        )
        return runBlocking { client.execute(hentPersonQuery) }
    }
}