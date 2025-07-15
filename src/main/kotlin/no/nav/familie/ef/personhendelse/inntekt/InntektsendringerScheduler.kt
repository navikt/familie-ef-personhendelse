package no.nav.familie.ef.personhendelse.inntekt

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class InntektsendringerScheduler(
    val inntektsendringerService: InntektsendringerService,
    val inntektOppgaveService: InntektOppgaveService,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "\${INNTEKTSKONTROLL_CRON_EXPRESSION}") // kl 04:00 den 6. hver måned
    fun inntektskontroll() {
        logger.info("Cron scheduler starter inntektskontroll")
        inntektsendringerService.beregnInntektsendringerOgLagreIDb()
        // Send med alle som har 10% eller mer i inntektsendring 3 mnd på rad
        inntektOppgaveService.opprettOppgaverForInntektsendringer(true)
        inntektOppgaveService.opprettOppgaverForNyeVedtakUføretrygd()
        inntektsendringerService.hentPersonerMedInntektsendringerOgRevurderAutomatisk()
    }
}
