package no.nav.familie.ef.personhendelse.sikkerhet

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.ef.personhendelse.configuration.RolleConfig
import org.springframework.stereotype.Component
import org.springframework.web.servlet.AsyncHandlerInterceptor

@Component
class TilgangInterceptor(
    private val rolleConfig: RolleConfig,
) : AsyncHandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean =
        if (SikkerhetContext.harTilgangTilGittRolle(rolleConfig)) {
            super.preHandle(request, response, handler)
        } else {
            throw Exception("Mangler nødvendige tilganger for ef-personhendelse.")
        }
}
