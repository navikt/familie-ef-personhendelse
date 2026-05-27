package no.nav.familie.ef.personhendelse.sikkerhet

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

/**
 * Bro-implementasjon av nav-token-support sitt TokenValidationContextHolder-grensesnitt
 * som er støttet av Spring Security sin SecurityContextHolder. Påkrevd for familie-felles:rest-klient
 * som bruker OIDCUtil → TokenValidationContextHolder for logging/videreføring av token.
 *
 * TODO: Kan fjernes når familie-felles oppdateres til å bruke Spring Security direkte.
 */
@Component
class SpringSecurityTokenValidationContextHolder : TokenValidationContextHolder {
    override fun getTokenValidationContext(): TokenValidationContext {
        val auth =
            SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
                ?: return TokenValidationContext(emptyMap())
        return TokenValidationContext(mapOf("azuread" to JwtToken(auth.token.tokenValue)))
    }

    override fun setTokenValidationContext(tokenValidationContext: TokenValidationContext?) {
        // no-op: kontekst håndteres av Spring Security
    }
}
