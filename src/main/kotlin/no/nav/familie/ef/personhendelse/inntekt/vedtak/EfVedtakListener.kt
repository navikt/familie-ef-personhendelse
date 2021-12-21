package no.nav.familie.ef.personhendelse.inntekt.vedtak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.ef.EnsligForsørgerVedtakhendelse
import no.nav.familie.kontrakter.felles.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
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
        id = "familie-ef-personhendelse-vedtak",
        topics = ["\${FAMILIE_EF_VEDTAK_TOPIC}"]
    )
    fun listen(consumerRecord: ConsumerRecord<String, String>) {
        val efVedtakHendelse = objectMapper.readValue<EnsligForsørgerVedtakhendelse>(consumerRecord.value())
        try {
            efVedtakRepository.lagreEfVedtakHendelse(efVedtakHendelse)
        } catch (e: Exception) {
            logger.error("Feil ved håndtering av personhendelse med behandlingId: ${efVedtakHendelse.behandlingId}")
            securelogger.error(
                "Feil ved håndtering av personhendelse med behandlingId ${efVedtakHendelse.behandlingId}: ${e.message}" +
                    " hendelse={}",
                objectMapper.writeValueAsString(efVedtakHendelse)
            )
            throw e
        }
    }
}
