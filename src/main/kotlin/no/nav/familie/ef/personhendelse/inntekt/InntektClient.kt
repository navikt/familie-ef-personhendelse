package no.nav.familie.ef.personhendelse.inntekt

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
    private fun lagInntektUri(
        fom: YearMonth,
        tom: YearMonth,
    ) = UriComponentsBuilder.fromUri(uri).pathSegment("api/inntekt")
        .queryParam("fom", fom)
        .queryParam("tom", tom)
        .build().toUri()

    private fun lagInntekthistorikkUri(
        fom: YearMonth,
        tom: YearMonth?,
    ) = UriComponentsBuilder.fromUri(uri).pathSegment("api/inntekt/historikk")
        .queryParam("fom", fom)
        .queryParam("tom", tom)
        .build().toUri()

    fun hentInntekt(
        personIdent: String,
        fom: YearMonth,
        tom: YearMonth,
    ): HentInntektListeResponse {
        return postForEntity(lagInntektUri(fom, tom), PersonIdent(personIdent))
    }

    fun hentInntektshistorikk(
        personIdent: String,
        fom: YearMonth,
        tom: YearMonth?,
    ): InntektshistorikkResponse {
        return postForEntity(lagInntekthistorikkUri(fom, tom), PersonIdent(personIdent))
    }
}
