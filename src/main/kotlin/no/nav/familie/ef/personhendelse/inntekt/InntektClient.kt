package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.inntekt.inntektv2.InntektResponse
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.PersonIdent
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

    private fun lagInntekthistorikkUri(
        fom: YearMonth,
        tom: YearMonth?,
    ) = UriComponentsBuilder
        .fromUri(uri)
        .pathSegment("api/inntekt/historikk")
        .queryParam("fom", fom)
        .queryParam("tom", tom)
        .build()
        .toUri()

    fun hentInntekt(
        personident: String,
        fom: YearMonth,
        tom: YearMonth,
    ): InntektResponse =
        postForEntity(
            uri = inntektV2Uri,
            payload =
                HentInntektPayload(
                    personident = personident,
                    maanedFom = fom,
                    maanedTom = tom,
                ),
        )

    fun hentInntektshistorikk(
        personIdent: String,
        fom: YearMonth,
        tom: YearMonth?,
    ): InntektshistorikkResponse = postForEntity(lagInntekthistorikkUri(fom, tom), PersonIdent(personIdent))
}

data class HentInntektPayload(
    val personident: String,
    val maanedFom: YearMonth,
    val maanedTom: YearMonth,
)
