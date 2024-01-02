package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.leader.LeaderClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class InntektsendringerScheduler(val vedtakendringerService: VedtakendringerService) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    @Scheduled(cron = "\${INNTEKTSKONTROLL_CRON_EXPRESSION}") // kl 04:00 den 6. hver måned
    fun inntektskontroll() {
        logger.info("Cron scheduler starter inntektskontroll")
        if (LeaderClient.isLeader() == true) {
            logger.info("Leader-pod starter inntektskontroll")
            vedtakendringerService.beregnInntektsendringerOgLagreIDb()
            vedtakendringerService.opprettOppgaverForInntektsendringer(true)
        }
    }
}
