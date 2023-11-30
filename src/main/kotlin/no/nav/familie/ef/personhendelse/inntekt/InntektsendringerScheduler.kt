package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.leader.LeaderClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class InntektsendringerScheduler(val vedtakendringerService: VedtakendringerService) {

    @Scheduled(cron = "0 0 4 6 * *") // kl 04:00 den 6. hver måned
    fun inntektskontroll() {
        if (LeaderClient.isLeader() == true) {
            vedtakendringerService.beregnInntektsendringerOgLagreIDb()
            vedtakendringerService.opprettOppgaverForInntektsendringer(true)
        }
    }
}
