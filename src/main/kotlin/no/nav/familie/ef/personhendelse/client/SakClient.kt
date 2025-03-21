package no.nav.familie.ef.personhendelse.client

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.ef.personhendelse.NyeBarnDto
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Component
class SakClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${EF_SAK_URL}")
    private val uri: URI,
) : AbstractRestClient(restOperations, "familie.ef-sak") {
    fun harLøpendeStønad(personidenter: Set<String>): Boolean {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/ekstern/behandling/har-loepende-stoenad")
        val response = postForEntity<Ressurs<Boolean>>(uriComponentsBuilder.build().toUri(), personidenter)
        return response.data ?: error("Kall mot ef-sak feilet. Status=${response.status} - ${response.melding}")
    }

    fun harLøpendeBarnetilsyn(personident: String): Boolean {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/ekstern/behandling/har-loepende-barnetilsyn")
        val response = postForEntity<Ressurs<Boolean>>(uriComponentsBuilder.build().toUri(), PersonIdent(personident))
        return response.data ?: error("Kall mot ef-sak feilet. Status=${response.status} - ${response.melding}")
    }

    fun inntektForEksternId(eksternId: Long): Int? {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/vedtak/eksternid/$eksternId/inntekt")
                .queryParam("dato", LocalDate.now())
        val response = getForEntity<Ressurs<Int?>>(uriComponentsBuilder.build().toUri())
        return response.data
    }

    fun harAktivtVedtak(eksternId: Long): Boolean {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/vedtak/eksternid/$eksternId/harAktivtVedtak")
                .queryParam("dato", LocalDate.now())
        val response = getForEntity<Ressurs<Boolean>>(uriComponentsBuilder.build().toUri())
        return response.data ?: throw Exception("Feil ved kall, mottok NULL: harAktivtVedtak skal alltid returnere en verdi")
    }

    fun hentAlleAktiveIdenterOgForventetInntekt(): Map<String, Int?> {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/vedtak/gjeldendeIverksatteBehandlingerMedInntekt")
        val response = getForEntity<Ressurs<Map<String, Int?>>>(uriComponentsBuilder.build().toUri())
        return response.data
            ?: throw Exception("Feil ved kall mot ef-sak ved henting av forventet inntekt for personer med aktiv stønad")
    }

    fun hentPersonerMedAktivStønadIkkeManueltRevurdertSisteMåneder(antallMåneder: Int = 3): List<String> {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/vedtak/personerMedAktivStonadIkkeManueltRevurdertSisteMaaneder")
                .queryParam("antallMaaneder", antallMåneder)
        val response = getForEntity<Ressurs<List<String>>>(uriComponentsBuilder.build().toUri())
        return response.data
            ?: throw Exception("Feil ved kall mot ef-sak ved henting av forventet inntekt for personer med aktiv stønad")
    }

    fun hentForventetInntektForIdenter(personidenter: Collection<String>): List<ForventetInntektForPerson> {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/vedtak/gjeldendeIverksatteBehandlingerMedInntekt")
        val response = postForEntity<Ressurs<List<ForventetInntektForPerson>>>(uriComponentsBuilder.build().toUri(), personidenter)
        return response.data
            ?: throw Exception("Feil ved kall mot ef-sak ved henting av forventet inntekt for personer med aktiv stønad")
    }

    fun finnNyeBarnForBruker(personIdent: PersonIdent): NyeBarnDto {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/behandling/barn/nye-eller-tidligere-fodte-barn")
        val response = postForEntity<Ressurs<NyeBarnDto>>(uriComponentsBuilder.build().toUri(), personIdent)
        return response.getDataOrThrow()
    }

    fun revurderAutomatisk(personIdenter: List<String>): List<AutomatiskRevurdering>? {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/automatisk-revurdering")
        val response = postForEntity<Ressurs<List<AutomatiskRevurdering>>>(uriComponentsBuilder.build().toUri(), personIdenter)
        return response.data
    }

    fun opprettBehandleAutomatiskInntektsendringTask(personIdenter: List<String>): Boolean {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/revurdering/behandle-automatisk")
        val response = postForEntity<Ressurs<Boolean>>(uriComponentsBuilder.build().toUri(), personIdenter)
        return response.data ?: throw Exception("Feil ved kall mot ef-sak med personIdenter for oppretting av behandle automatisk inntektsendring task.")
    }
}

data class ForventetInntektForPerson(
    val personIdent: String,
    val forventetInntektForrigeMåned: Int?,
    val forventetInntektToMånederTilbake: Int?,
    val forventetInntektTreMånederTilbake: Int?,
    val forventetInntektFireMånederTilbake: Int?,
)

data class AutomatiskRevurdering(
    val personIdent: String,
    val automatiskRevurdert: Boolean,
)
