package no.nav.familie.ef.personhendelse.inntekt

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.YearMonth

@Component
class InntektClient(
    @Value("\${INNTEKT_URL}") private val uri: URI,
    @Qualifier("inntektRestClient") private val restClient: RestClient,
) {
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

        return restClient
            .post()
            .uri(inntektV2Uri)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .body<InntektResponse>()!!
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
