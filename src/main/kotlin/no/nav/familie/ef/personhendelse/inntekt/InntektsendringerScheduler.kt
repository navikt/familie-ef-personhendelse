package no.nav.familie.ef.personhendelse.inntekt

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class InntektsendringerScheduler(
    val inntektsendringerService: InntektsendringerService,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "\${INNTEKTSKONTROLL_CRON_EXPRESSION}") // kl 04:00 den 6. hver måned
    fun inntektskontroll() {
        logger.info("Cron scheduler starter inntektskontroll")
        inntektsendringerService.beregnInntektsendringerOgLagreIDb()
        // Send med alle som har 10% eller mer i inntektsendring 3 mnd på rad
        inntektsendringerService.opprettOppgaverForInntektsendringer(true)
        inntektsendringerService.opprettOppgaverForNyeVedtakUføretrygd()
    }
}

/*

Ikke varierende inntekt - Personhendelse

Ikke næringsinntekt. Bruk integrasjonen Sigrun til dette. - EF-Sak

Ikke ubehandlet søknad på samme bruker - EF sak

Ikke journalføringsoppgave på samme bruker - EF sak

Ikke åpen behandling på samme bruker - EF sak

Ingen andre løpende stønader som likestilles med arbeidsinntekt + samordning (dette har vi gode måter å sjekke allerede. Løsning for dette finnes i personhendelse, hvor det finnes kode som finner hvilken stønad som er ny for bruker ved bruk av a-inntekt)

*/
