package no.nav.familie.ef.personhendelse.inntekt

import io.swagger.v3.oas.annotations.Operation
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/inntekt/forvaltning/")
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class InntektsEndringForvaltningerController(
    private val inntektsendringerService: InntektsendringerService,
) {
    @Operation(summary = "Manuell forvaltningsendepunkt for opprettelse av BehandleAutomatiskInntektsendringTask.")
    @PostMapping("behandle-automatisk-inntektsendring")
    fun lagBehandleAutomatiskInntektsendringTask(
        @RequestBody behandleAutomatiskInntektsendringTaskRequestBody: BehandleAutomatiskInntektsendringTaskRequestBody,
    ) {
        inntektsendringerService.manuellOpprettBehandleAutomatiskInntektsendringTask(behandleAutomatiskInntektsendringTaskRequestBody.personIdent)
    }
}

data class BehandleAutomatiskInntektsendringTaskRequestBody(
    val personIdent: String,
)
