package no.nav.familie.ef.personhendelse.sikkerhet

import no.nav.familie.ef.personhendelse.configuration.RolleConfig
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

object SikkerhetContext {
    fun hentGrupperFraToken(): Set<String> =
        Result
            .runCatching {
                SpringTokenValidationContextHolder().getTokenValidationContext()
            }.fold(
                onSuccess = { tokenValidationContext ->
                    @Suppress("UNCHECKED_CAST")
                    val groups = tokenValidationContext.getClaims("azuread").get("groups") as List<String>?
                    groups?.toSet() ?: emptySet()
                },
                onFailure = {
                    emptySet()
                },
            )

    fun harTilgangTilGittRolle(
        rolleConfig: RolleConfig,
    ): Boolean {
        val rollerFraToken = hentGrupperFraToken()
        return rollerFraToken.contains(rolleConfig.forvalter)
    }

    fun harRolle(rolleId: String): Boolean = hentGrupperFraToken().contains(rolleId)
}
