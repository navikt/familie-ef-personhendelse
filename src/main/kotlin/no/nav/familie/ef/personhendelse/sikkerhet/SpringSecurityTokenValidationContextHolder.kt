package no.nav.familie.ef.personhendelse.sikkerhet

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

/**
 * Bridge that implements the nav-token-support TokenValidationContextHolder interface
 * backed by Spring Security's SecurityContextHolder. Required for familie-felles:rest-klient
 * which uses OIDCUtil → TokenValidationContextHolder for logging/propagation.
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
        // no-op: context is managed by Spring Security
    }
}
