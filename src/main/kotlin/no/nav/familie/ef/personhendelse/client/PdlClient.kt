package no.nav.familie.ef.personhendelse.client

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import kotlinx.coroutines.runBlocking
import no.nav.familie.ef.personhendelse.generated.HentPerson
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentPerson(fnr: String, callId: String): GraphQLClientResponse<HentPerson.Result> {

        val variables = HentPerson.Variables(fnr, true, true)
        val hentPersonQuery = HentPerson(variables)

        val token = azureClient.hentToken(scope)
        val client = GraphQLWebClient(
            url = url,
            builder = WebClient.builder().defaultHeaders {
                it.setBearerAuth(token)
                it.add("Tema", "ENF")
            }
        )
        return runBlocking { client.execute(hentPersonQuery) }
    }
}