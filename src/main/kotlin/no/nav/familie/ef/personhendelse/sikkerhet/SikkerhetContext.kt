package no.nav.familie.ef.personhendelse.sikkerhet

import no.nav.familie.ef.personhendelse.configuration.RolleConfig
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

object SikkerhetContext {
    fun hentGrupperFraToken(): Set<String> =
        Result
            .runCatching {
                val authentication = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
                authentication?.token?.getClaimAsStringList("groups")?.toSet() ?: emptySet()
            }.getOrDefault(emptySet())

    fun harTilgangTilGittRolle(
        rolleConfig: RolleConfig,
    ): Boolean {
        val rollerFraToken = hentGrupperFraToken()
        return rollerFraToken.contains(rolleConfig.forvalter)
    }

    fun harRolle(rolleId: String): Boolean = hentGrupperFraToken().contains(rolleId)
}
