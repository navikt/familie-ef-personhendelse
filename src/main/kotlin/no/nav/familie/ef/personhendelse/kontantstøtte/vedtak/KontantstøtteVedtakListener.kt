package no.nav.familie.ef.personhendelse.kontantstøtte.vedtak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.familie.kontrakter.felles.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.stereotype.Component

@Component
class KontantstøtteVedtakListener(val kontantstøtteVedtakService: KontantstøtteVedtakService) : ConsumerSeekAware {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val securelogger = LoggerFactory.getLogger("secureLogger")
    private val vurderKonsekvensOppgaveBeskrivelse =
        "Bruker har fått vedtak om kontantstøtte og har løpende barnetilsyn"

    @KafkaListener(
        id = "aapen-kontantstotte-vedtak-aiven",
        groupId = "familie-ef-kontantstøtte-vedtak",
        topics = ["\${FAMILIE_KS_VEDTAK_TOPIC}"],
        containerFactory = "kafkaKontantstøtteVedtakListenerContainerFactory",
    )
    fun listen(consumerRecord: ConsumerRecord<String, String>) {
        val vedtakhendelse = objectMapper.readValue<VedtakDVH>(consumerRecord.value())
        try {
            logger.info("Leser vedtak for kontantstøtte med behandlingId: ${vedtakhendelse.behandlingsId}")
            val personIdent = vedtakhendelse.person.personIdent
            if (kontantstøtteVedtakService.harLøpendeBarnetilsyn(personIdent)) {
                kontantstøtteVedtakService.opprettVurderKonsekvensOppgaveForBarnetilsyn(
                    personIdent = personIdent,
                    vurderKonsekvensOppgaveBeskrivelse,
                )
            }
        } catch (e: Exception) {
            logger.error("Feil ved håndtering av kontantstøttevedtak - se securelogs for mer detaljer")
            securelogger.error(
                "Feil ved håndtering av vedtakhendelse med behandlingsId ${vedtakhendelse.behandlingsId} med ytelse kontantstøtte : ${e.message}" +
                    " hendelse={}",
                objectMapper.writeValueAsString(vedtakhendelse),
                e,
            )
            throw e
        }
    }

    /**
     * TODO : Bør kommenteres ut etter første deploy for å ikke kunne trigge seekToEnd
     */
    override fun onPartitionsAssigned(
        assignments: MutableMap<org.apache.kafka.common.TopicPartition, Long>,
        callback: ConsumerSeekAware.ConsumerSeekCallback,
    ) {
        logger.info("overrided onPartitionsAssigned seekToBeginning")
        assignments.keys.stream()
            .filter { it.topic() == "teamfamilie.aapen-kontantstotte-vedtak-v1" }
            .forEach {
                // callback.seekToBeginning("teamfamilie.aapen-kontantstotte-vedtak-v1", it.partition())
                callback.seekToEnd("teamfamilie.aapen-kontantstotte-vedtak-v1", it.partition())
            }
    }
}
