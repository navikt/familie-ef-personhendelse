package no.nav.familie.ef.personhendelse.forvaltning

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.inntekt.InntektOppgaveService
import no.nav.familie.ef.personhendelse.inntekt.InntektsendringerService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/revurdering/forvaltning/inntektsendring")
@ProtectedWithClaims(issuer = "azuread")
class BehandleAutomatiskInntektsendringForvaltningsController(
    private val sakClient: SakClient,
    private val inntektsendringerService: InntektsendringerService,
    private val inntektOppgaveService: InntektOppgaveService,
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
        val person = listOf(personIdent)
        sakClient.revurderAutomatisk(person)
    }

    @Operation(
        description =
            "Utfører en dry-run av inntektskontroll. Det vil si at hele inntektskontroll kjøres, med unntak av opprettelse av oppgaver. " +
                "Det betyr at databasen vil oppdateres, altså at forrige kjøring vil overskrives. Dette for å teste hele flyten.",
        summary =
            "Dry-run av inntektskontroll",
    )
    @GetMapping("/dry-run-inntektskontroll")
    fun dryRunInntektskontroll() {
        inntektsendringerService.beregnInntektsendringerAsync()
    }

    @Operation(
        description =
            "Oppprett oppgaver for uføretrygdsendringer",
        summary =
            "Oppretter oppgaver for uføretrygdsendringer for personer som har hatt endring i stønadsbeløpet i løpet av de 2 siste månedene. " +
                "Baserer seg på inntektskontroll og krever derfor en dry-run av inntektskontroll for å hente nyeste data.",
    )
    @GetMapping("/opprett-oppgaver-for-uføretrygdsendringer")
    fun opprettOppgaverForUføretrygdsendringer() {
        inntektOppgaveService.opprettOppgaverForInntektsendringerAsync(true)
    }

    @Operation(
        description =
            "Oppretter oppgaver på samme måte som i scheduler etter inntektskontrollen. Kjør dry-run av inntektskontroll først for oppdaterte verdier i databasen.",
        summary =
            "Oppretter oppgaver og automatisk revurderer på bakgrunn av hva som er hentet inn i inntektskontrollen.",
    )
    @GetMapping("/opprett-oppgaver-fra-inntektskontroll")
    fun opprettOppgaverFraInntektskontroll() {
        // Send med alle som har 10% eller mer i inntektsendring 3 mnd på rad
        inntektOppgaveService.opprettOppgaverForInntektsendringer(true)
        inntektOppgaveService.opprettOppgaverForNyeVedtakUføretrygd()
        inntektsendringerService.hentPersonerMedInntektsendringerOgRevurderAutomatisk()
    }

    @Operation(
        description =
            "Utfør automatisk revurdering mot ef-sak for personer med inntektsendring, altså 10% endring i inntekt siste 3 måneder. " +
                "Dobbeltsjekk at featuretoggle familie.ef.sak-behandle-automatisk-inntektsendring-task er av i prod dersom man kun ønsker å logge",
        summary =
            "Utfør automatisk revurdering mot ef-sak for personer med inntektsendring",
    )
    @GetMapping("/revurder-personer-med-inntektsendringer-automatisk")
    fun hentPersonerMedInntektsendringerOgRevurderAutomatisk() {
        inntektsendringerService.hentPersonerMedInntektsendringerOgRevurderAutomatisk()
    }
}
