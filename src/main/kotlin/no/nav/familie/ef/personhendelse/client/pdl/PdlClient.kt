package no.nav.familie.ef.personhendelse.client.pdl

import no.nav.familie.ef.personhendelse.generated.HentIdenter
import no.nav.familie.ef.personhendelse.generated.HentPerson
import no.nav.familie.ef.personhendelse.generated.hentidenter.IdentInformasjon
import no.nav.familie.ef.personhendelse.generated.hentperson.Person
import no.nav.familie.http.client.AbstractRestClient
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
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
    val hentIdenter = javaClass.getResource("/pdl/queries/hentIdenter.graphql").readText().graphqlCompatible()

    fun hentPerson(fnr: String): Person {

        val pdlPersonRequest = PdlPersonRequest(
            variables = PdlPersonRequestVariables(fnr),
            query = hentPersonQuery
        )

        val pdlResponse: PdlResponse<HentPerson.Result> = postForEntity(pdlUri, pdlPersonRequest, httpHeadersPdl())
        return feilsjekkOgReturnerData(fnr, pdlResponse) { it.hentPerson }
    }

    fun hentIdenter(personIdent: String): Set<String> {
        val pdlPersonRequest = PdlPersonRequest(
                variables = PdlIdentRequestVariables(personIdent),
                query = hentIdenter
        )

        val pdlResponse: PdlResponse<HentIdenter.Result> = postForEntity(pdlUri, pdlPersonRequest, httpHeadersPdl())
        return feilsjekkOgReturnerData(personIdent, pdlResponse) { it.hentIdenter }.identer.map(IdentInformasjon::ident).toSet()
    }
}

private fun httpHeadersPdl(): HttpHeaders {

    return HttpHeaders().apply {
        add("Tema", "ENF")
    }
}

fun String.graphqlCompatible(): String {
    return StringUtils.normalizeSpace(this.replace("\n", ""))
}