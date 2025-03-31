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
class InntektController(
    val inntektsendringerService: InntektsendringerService,
) {
    @GetMapping("/sjekkEndringer")
    fun sjekkEndringer(): ResponseEntity<Any> {
        inntektsendringerService.beregnInntektsendringerAsync()
        return ResponseEntity.ok().build()
    }

    @GetMapping("/opprettOppgaver")
    fun opprettOppgaverForInntektsendringer(
        @RequestParam skalOppretteOppgaver: Boolean,
    ): ResponseEntity<Int> {
        val antallOppgaver = inntektsendringerService.opprettOppgaverForInntektsendringer(skalOppretteOppgaver)
        inntektsendringerService.opprettOppgaverForNyeVedtakUføretrygd()
        return ResponseEntity.ok(antallOppgaver)
    }
}
