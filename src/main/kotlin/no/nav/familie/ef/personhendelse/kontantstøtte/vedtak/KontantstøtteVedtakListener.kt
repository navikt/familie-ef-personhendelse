package no.nav.familie.ef.personhendelse.kontantstøtte.vedtak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.familie.kontrakter.felles.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class KontantstøtteVedtakListener(val kontantstøtteVedtakService: KontantstøtteVedtakService) : ConsumerSeekAware {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val securelogger = LoggerFactory.getLogger("secureLogger")

    @KafkaListener(
        id = "aapen-kontantstotte-vedtak-aiven",
        groupId = "familie-ef-kontantstøtte-vedtak",
        topics = ["teamfamilie.aapen-kontantstotte-vedtak-v1"],
        containerFactory = "kafkaVedtakListenerContainerFactory",
    )
    fun listen(@Payload consumerRecord: ConsumerRecord<String, String>) {
        val vedtakhendelse = objectMapper.readValue<VedtakDVH>(consumerRecord.value())
        val personIdent = vedtakhendelse.person.personIdent
        try {
            logger.info("Lest vedtak for kontantstøtte med behandlingId: ${vedtakhendelse.behandlingsId}")
            if (kontantstøtteVedtakService.harLøpendeBarnetilsyn(personIdent)) {
                //kontantstøtteVedtakService.opprettVurderKonsekvensOppgaveForBarnetilsyn(personIdent, "Bruker har fått vedtak om kontantstøtte og har løpende barnetilsyn")
                //logger.info("Opprettet VurderKonsekvensOppgave for kontantstøttevedtak med behandlingId: ${vedtakhendelse.behandlingsId}")
            }
        } catch (e: Exception) {
            logger.error("Feil ved håndtering av kontantstøttevedtak - se securelogs for mer detaljer")
            securelogger.error(
                "Feil ved håndtering av vedtakhendelse med behandlingsId ${vedtakhendelse.behandlingsId} med ytelse kontantstøtte : ${e.message}" +
                    " hendelse={}",
                objectMapper.writeValueAsString(vedtakhendelse), e
            )
            throw e
        }
    }

    /**
     * TODO : Må kommenteres ut etter første deploy for å ikke søke tilbake til siste melding hver gang
     */
    override fun onPartitionsAssigned(
        assignments: MutableMap<org.apache.kafka.common.TopicPartition, Long>,
        callback: ConsumerSeekAware.ConsumerSeekCallback
    ) {
        logger.info("overrided onPartitionsAssigned seekToBeginning")
        assignments.keys.stream()
            .filter { it.topic() == "teamfamilie.aapen-kontantstotte-vedtak-v1" }
            .forEach {
                callback.seekToBeginning("teamfamilie.aapen-kontantstotte-vedtak-v1", it.partition())
                //callback.seekToEnd("teamfamilie.aapen-kontantstotte-vedtak-v1", it.partition())
            }
    }
}
