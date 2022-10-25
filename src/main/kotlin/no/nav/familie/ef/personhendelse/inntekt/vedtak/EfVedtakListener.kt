package no.nav.familie.ef.personhendelse.inntekt.vedtak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.ef.EnsligForsørgerVedtakhendelse
import no.nav.familie.kontrakter.felles.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
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
        topics = ["\${FAMILIE_EF_VEDTAK_TOPIC}"],
        containerFactory = "kafkaVedtakListenerContainerFactory",
        groupId = "familie-ef-personhendelse-vedtak"
    )
    fun listen(consumerRecord: ConsumerRecord<String, String>) {
        val efVedtakshendelse = objectMapper.readValue<EnsligForsørgerVedtakhendelse>(consumerRecord.value())
        try {
            efVedtakRepository.lagreEfVedtakshendelse(efVedtakshendelse)
        } catch (e: Exception) {
            logger.error("Feil ved håndtering av personhendelse med behandlingId: ${efVedtakshendelse.behandlingId}")
            securelogger.error(
                "Feil ved håndtering av personhendelse med behandlingId ${efVedtakshendelse.behandlingId}: ${e.message}" +
                    " hendelse={}",
                objectMapper.writeValueAsString(efVedtakshendelse)
            )
            throw e
        }
    }


    override fun onPartitionsAssigned(
        assignments: MutableMap<org.apache.kafka.common.TopicPartition, Long>,
        callback: ConsumerSeekAware.ConsumerSeekCallback
    ) {
        logger.info("overrided onPartitionsAssigned seekToBeginning")
        assignments.keys.stream()
            .filter { it.topic() == "teamfamilie.aapen-ensligforsorger-iverksatt-vedtak" }
            .forEach {
                callback.seekRelative("teamfamilie.aapen-ensligforsorger-iverksatt-vedtak", it.partition(), -1, false)
                // callback.seekToBeginning("aapen-person-pdl-leesah-v1", it.partition())
            }
    }
}
