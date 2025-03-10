package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.YearMonth

@Component
class InntektClient(
    @Value("\${FAMILIE_EF_PROXY_URL}") private val uri: URI,
    @Qualifier("azure") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "inntekt") {
    private val inntektV2Uri =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("api/inntekt/v2")
            .build()
            .toUri()

    fun hentInntekt(
        personIdent: String,
        månedFom: YearMonth,
        månedTom: YearMonth,
    ): InntektResponse =
        postForEntity(
            uri = inntektV2Uri,
            payload =
                HentInntektPayload(
                    personIdent = personIdent,
                    månedFom = månedFom,
                    månedTom = månedTom,
                ),
        )
}

data class HentInntektPayload(
    val personIdent: String,
    val månedFom: YearMonth,
    val månedTom: YearMonth,
)
