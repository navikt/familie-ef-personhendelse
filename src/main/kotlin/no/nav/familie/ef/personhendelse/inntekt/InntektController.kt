package no.nav.familie.ef.personhendelse.inntekt

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/inntekt"])
@ProtectedWithClaims(issuer = "azuread")
class InntektController(val vedtakendringer: VedtakendringerService) {

    @GetMapping("/sjekkEndringer")
    fun sjekkEndringer(@RequestParam skalOppretteOppgaver: Boolean ): ResponseEntity<Any> {
        vedtakendringer.beregnNyeVedtakOgLagOppgave(skalOppretteOppgaver)
        return ResponseEntity.ok().build()
    }
}
