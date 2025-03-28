package no.nav.familie.ef.personhendelse.forvaltning

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.pdl.secureLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/revurdering/forvaltning/inntektsendring")
@ProtectedWithClaims(issuer = "azuread")
class BehandleAutomatiskInntektsendringForvaltningsController(
    private val sakClient: SakClient,
) {
    @Operation(
        description = "Kan brukes til å opprette en automatisk behandle automatisk inntektsendring gjennom ef-sak for en person.",
        summary =
            "Skal kunne manuelt opprette en behandling for automatisk inntektsendring for en person.",
    )
    @PostMapping("/manuelt-opprett")
    fun manueltOpprettBehandleAutomatiskInntektsendringTask(
        @RequestBody personIdent: String,
    ) {
        // TODO: Dette skal fjernes, kun brukt for debug.
        if (personIdent.isBlank()) {
            secureLogger.error("Klarte ikke å hente ut personIdent fra request body.")
        } else {
            secureLogger.info("Sender personIdent: $personIdent for  oppretting av behandle automatisk inntektsendring task i ef-sak.")
        }

        val person = listOf(personIdent)
        sakClient.revurderAutomatisk(person)
    }
}
