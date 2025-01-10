package no.nav.familie.ef.personhendelse.inntekt

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class InntektsendringerScheduler(
    val vedtakendringerService: VedtakendringerService,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "\${INNTEKTSKONTROLL_CRON_EXPRESSION}") // kl 04:00 den 6. hver måned
    fun inntektskontroll() {
        logger.info("Cron scheduler starter inntektskontroll")
        vedtakendringerService.beregnInntektsendringerOgLagreIDb()
        // EF-sak: Send med kandidater til automatisk behandling
        vedtakendringerService.opprettOppgaverForInntektsendringer(true)
        vedtakendringerService.opprettOppgaverForNyeVedtakUføretrygd()
    }
}
