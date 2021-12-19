package no.nav.familie.ef.personhendelse.inntekt.vedtak

import no.nav.familie.kontrakter.felles.ef.EnsligForsørgerVedtakhendelse
import no.nav.familie.kontrakter.felles.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Lytter på hendelser som produseres av ef-iverksett som lages når vedtak iverksettes
 */
@Component
class EfVedtakListener(
    private val efVedtakRepository: EfVedtakRepository
) : ConsumerSeekAware {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val securelogger = LoggerFactory.getLogger("secureLogger")

    @KafkaListener(
        id = "familie-ef-vedtak",
        topics = ["\${FAMILIE_EF_VEDTAK_TOPIC}"],
        containerFactory = "kafkaEfVedtakListenerContainerFactory"
    )
    fun listen(@Payload vedtakHendelse: EnsligForsørgerVedtakhendelse) {
        try {
            efVedtakRepository.lagreEfVedtakHendelse(vedtakHendelse)
        } catch (e: Exception) {
            logger.error("Feil ved håndtering av personhendelse med behandlingId: ${vedtakHendelse.behandlingId}")
            securelogger.error(
                "Feil ved håndtering av personhendelse med behandlingId ${vedtakHendelse.behandlingId}: ${e.message}" +
                    " hendelse={}",
                objectMapper.writeValueAsString(vedtakHendelse)
            )
            throw e
        }
    }
}
