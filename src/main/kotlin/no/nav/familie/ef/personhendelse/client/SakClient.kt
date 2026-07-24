package no.nav.familie.ef.personhendelse.client

import no.nav.familie.kontrakter.ef.personhendelse.NyeBarnDto
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Component
class SakClient(
    @Qualifier("efSakRestClient")
    private val restClient: RestClient,
    @Value("\${EF_SAK_URL}")
    private val uri: URI,
) {
    fun harLøpendeStønad(personidenter: Set<String>): Boolean {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/ekstern/behandling/har-loepende-stoenad")
        val response =
            restClient
                .post()
                .uri(uriComponentsBuilder.build().toUri())
                .body(personidenter)
                .retrieve()
                .body<Ressurs<Boolean>>()!!
        return response.data ?: error("Kall mot ef-sak feilet. Status=${response.status} - ${response.melding}")
    }

    fun harLøpendeBarnetilsyn(personident: String): Boolean {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/ekstern/behandling/har-loepende-barnetilsyn")
        val response =
            restClient
                .post()
                .uri(uriComponentsBuilder.build().toUri())
                .body(PersonIdent(personident))
                .retrieve()
                .body<Ressurs<Boolean>>()!!
        return response.data ?: error("Kall mot ef-sak feilet. Status=${response.status} - ${response.melding}")
    }

    fun inntektForEksternId(eksternId: Long): Int? {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/vedtak/eksternid/$eksternId/inntekt")
                .queryParam("dato", LocalDate.now())
        val response =
            restClient
                .get()
                .uri(uriComponentsBuilder.build().toUri())
                .retrieve()
                .body<Ressurs<Int?>>()!!
        return response.data
    }

    fun harAktivtVedtak(eksternId: Long): Boolean {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/vedtak/eksternid/$eksternId/harAktivtVedtak")
                .queryParam("dato", LocalDate.now())
        val response =
            restClient
                .get()
                .uri(uriComponentsBuilder.build().toUri())
                .retrieve()
                .body<Ressurs<Boolean>>()!!
        return response.data ?: throw Exception("Feil ved kall, mottok NULL: harAktivtVedtak skal alltid returnere en verdi")
    }

    fun hentAlleAktiveIdenterOgForventetInntekt(): Map<String, Int?> {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/vedtak/gjeldendeIverksatteBehandlingerMedInntekt")
        val response =
            restClient
                .get()
                .uri(uriComponentsBuilder.build().toUri())
                .retrieve()
                .body<Ressurs<Map<String, Int?>>>()!!
        return response.data
            ?: throw Exception("Feil ved kall mot ef-sak ved henting av forventet inntekt for personer med aktiv stønad")
    }

    fun hentPersonerMedAktivStønadIkkeManueltRevurdertSisteMåneder(antallMåneder: Int = 3): List<String> {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/vedtak/personerMedAktivStonadIkkeManueltRevurdertSisteMaaneder")
                .queryParam("antallMaaneder", antallMåneder)
        val response =
            restClient
                .get()
                .uri(uriComponentsBuilder.build().toUri())
                .retrieve()
                .body<Ressurs<List<String>>>()!!
        return response.data
            ?: throw Exception("Feil ved kall mot ef-sak ved henting av forventet inntekt for personer med aktiv stønad")
    }

    fun hentForventetInntektForIdenter(personidenter: Collection<String>): List<ForventetInntektForPerson> {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/vedtak/gjeldendeIverksatteBehandlingerMedInntekt")
        val response =
            restClient
                .post()
                .uri(uriComponentsBuilder.build().toUri())
                .body(personidenter)
                .retrieve()
                .body<Ressurs<List<ForventetInntektForPerson>>>()!!
        return response.data
            ?: throw Exception("Feil ved kall mot ef-sak ved henting av forventet inntekt for personer med aktiv stønad")
    }

    fun finnNyeBarnForBruker(personIdent: PersonIdent): NyeBarnDto {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/behandling/barn/nye-eller-tidligere-fodte-barn")
        val response =
            restClient
                .post()
                .uri(uriComponentsBuilder.build().toUri())
                .body(personIdent)
                .retrieve()
                .body<Ressurs<NyeBarnDto>>()!!
        return response.getDataOrThrow()
    }

    fun revurderAutomatisk(personIdenter: List<String>) {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/automatisk-revurdering")
        restClient
            .post()
            .uri(uriComponentsBuilder.build().toUri())
            .body(personIdenter)
            .retrieve()
            .toBodilessEntity()
    }

    fun revurderAutomatiskForvaltning(personIdenter: List<String>) {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/automatisk-revurdering/forvaltning")
        restClient
            .post()
            .uri(uriComponentsBuilder.build().toUri())
            .body(personIdenter)
            .retrieve()
            .toBodilessEntity()
    }
}

data class ForventetInntektForPerson(
    val personIdent: String,
    val forventetInntektForrigeMåned: Int?,
    val forventetInntektToMånederTilbake: Int?,
    val forventetInntektTreMånederTilbake: Int?,
    val forventetInntektFireMånederTilbake: Int?,
) {
    fun erSiste2MånederNotNull(): Boolean = this.forventetInntektForrigeMåned != null && this.forventetInntektToMånederTilbake != null
}

data class AutomatiskRevurdering(
    val personIdent: String,
    val automatiskRevurdert: Boolean,
)
