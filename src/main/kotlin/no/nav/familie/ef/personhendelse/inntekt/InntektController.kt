package no.nav.familie.ef.personhendelse.inntekt

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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

    @GetMapping("/automatisk-revurdering-dryrun")
    fun loggAutomatiskeInntektsendringer(): ResponseEntity<Any> {
        inntektsendringerService.loggAutomatiskeRevurderinger()
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

    // TODO: Denne skal fjernes, kun for testing gjennom Swagger.
    @PostMapping("/manuellOpprettelseAvBehandleAutomatiskInntektsendringTask")
    fun manuellOpprettelseAvBehandleAutomatiskInntektsendringTasker() {
        inntektsendringerService.opprettBehandleAutomatiskInntektsendringTask()
    }

    // TODO: Denne skal fjernes, kun for testing gjennom Swagger.
    @PostMapping("/manuellOpprettelseAvBehandleAutomatiskInntektsendringTasker")
    fun manuellOpprettelseAvBehandleAutomatiskInntektsendringTask(
        @RequestBody manuellOpprettelseAvBehandleAutomatiskInntektsendringTaskRequestBody: ManuellOpprettelseAvBehandleAutomatiskInntektsendringTaskRequestBody,
    ) {
        inntektsendringerService.manuellOpprettBehandleAutomatiskInntektsendringTask(manuellOpprettelseAvBehandleAutomatiskInntektsendringTaskRequestBody.personIdent)
    }
}

// TODO: Denne skal fjernes, kun for testing gjennom Swagger.
data class ManuellOpprettelseAvBehandleAutomatiskInntektsendringTaskRequestBody(
    val personIdent: String,
)
