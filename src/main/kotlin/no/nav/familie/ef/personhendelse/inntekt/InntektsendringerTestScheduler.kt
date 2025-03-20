package no.nav.familie.ef.personhendelse.inntekt

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@Profile("!prod")
class InntektsendringerTestScheduler(
    private val inntektsendringerService: InntektsendringerService,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    // Kjøres hver dag, hver 5 minutt.
    @Scheduled(cron = "\${INNTEKTSKONTROLL_TEST_CRON_EXPRESSION}")
    fun inntektskontroll() {
        logger.info("Cron scheduler starter for innkektskontroll tester scheduler.")
        inntektsendringerService.opprettBehandleAutomatiskInntektsendringTask()
    }
}
