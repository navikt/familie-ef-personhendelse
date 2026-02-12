package no.nav.familie.ef.personhendelse.client

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.restklient.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class ArbeidsfordelingClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${FAMILIE_INTEGRASJONER_API_URL}") private val integrasjonUri: String,
) : AbstractRestClient(restOperations, "familie.integrasjoner") {
    private val hentBehandlendeEnhetMedRelasjonerUrl = "$integrasjonUri/api/arbeidsfordeling/enhet/ENF/med-relasjoner"

    fun hentArbeidsfordelingEnhetId(ident: String): String? {
        val response =
            postForEntity<Ressurs<List<Arbeidsfordelingsenhet>>>(URI.create(hentBehandlendeEnhetMedRelasjonerUrl), PersonIdent(ident))
        return response.data?.firstOrNull()?.enhetId
    }
}

data class Arbeidsfordelingsenhet(
    val enhetId: String,
    val enhetNavn: String,
)
