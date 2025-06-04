package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.YearMonth

@Component
class InntektClient(
    @Value("\${INNTEKT_URL}") private val uri: URI,
    @Qualifier("azure") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "inntekt") {
    private val inntektV2Uri =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("rest/v2/inntekt")
            .build()
            .toUri()

    fun hentInntekt(
        personIdent: String,
        månedFom: YearMonth,
        månedTom: YearMonth,
    ): InntektResponse {
        val payload =
            genererInntektRequest(
                personIdent = personIdent,
                månedFom = månedFom,
                månedTom = månedTom,
            )

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                accept = listOf(MediaType.APPLICATION_JSON)
            }

        return postForEntity(
            uri = inntektV2Uri,
            payload = payload,
            httpHeaders = headers,
        )
    }
}

private fun genererInntektRequest(
    personIdent: String,
    månedFom: YearMonth,
    månedTom: YearMonth,
) = mapOf(
    "personident" to personIdent,
    "filter" to "StoenadEnsligMorEllerFarA-inntekt",
    "formaal" to "StoenadEnsligMorEllerFar",
    "maanedFom" to månedFom,
    "maanedTom" to månedTom,
)
