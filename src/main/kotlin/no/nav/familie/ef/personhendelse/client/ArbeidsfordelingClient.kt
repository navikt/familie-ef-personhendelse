package no.nav.familie.ef.personhendelse.client

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

@Component
class ArbeidsfordelingClient(
    @Qualifier("integrasjonerRestClient") private val restClient: RestClient,
    @Value("\${FAMILIE_INTEGRASJONER_API_URL}") private val integrasjonUri: String,
) {
    private val hentBehandlendeEnhetMedRelasjonerUrl = "$integrasjonUri/api/arbeidsfordeling/enhet/ENF/med-relasjoner"

    fun hentArbeidsfordelingEnhetId(ident: String): String? {
        val response =
            restClient
                .post()
                .uri(URI.create(hentBehandlendeEnhetMedRelasjonerUrl))
                .body(PersonIdent(ident))
                .retrieve()
                .body<Ressurs<List<Arbeidsfordelingsenhet>>>()!!
        return response.data?.firstOrNull()?.enhetId
    }
}

data class Arbeidsfordelingsenhet(
    val enhetId: String,
    val enhetNavn: String,
)
