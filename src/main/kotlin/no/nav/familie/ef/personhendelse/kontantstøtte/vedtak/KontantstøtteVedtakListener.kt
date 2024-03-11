package no.nav.familie.ef.personhendelse.kontantstøtte.vedtak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.familie.kontrakter.felles.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class KontantstøtteVedtakListener(val kontantstøtteVedtakService: KontantstøtteVedtakService) : ConsumerSeekAware {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val securelogger = LoggerFactory.getLogger("secureLogger")
    private val vurderKonsekvensOppgaveBeskrivelse =
        "Bruker har løpende stønad til barnetilsyn og har fått nytt vedtak om kontantstøtte. Sjekk om det nye vedtaket påvirker stønaden til barnetilsyn."

    @KafkaListener(
        id = "aapen-kontantstotte-vedtak-aiven",
        groupId = "familie-ef-kontantstøtte-vedtak",
        topics = ["\${FAMILIE_KS_VEDTAK_TOPIC}"],
        containerFactory = "kafkaKontantstøtteVedtakListenerContainerFactory",
    )
    fun listen(consumerRecord: ConsumerRecord<String, String>, ack: Acknowledgment) {
        val vedtakhendelse = objectMapper.readValue<VedtakDVH>(consumerRecord.value())
        try {
            logger.info("Leser vedtak for kontantstøtte med behandlingId: ${vedtakhendelse.behandlingsId}")
            if (erAlleredeHåndtert(vedtakhendelse)) {
                logger.info("Har allerede håndtert kontantstøttevedtak med behandlingId=${vedtakhendelse.behandlingsId}")
            } else {
                opprettOppgaveHvisPersonHarLøpendeBarnetilsyn(vedtakhendelse.person.personIdent, vedtakhendelse.behandlingsId)
            }
            ack.acknowledge()
        } catch (e: Exception) {
            logger.error("Feil ved håndtering av kontantstøttevedtak - se securelogs for mer detaljer")
            securelogger.error(
                "Feil ved håndtering av vedtakhendelse med behandlingsId ${vedtakhendelse.behandlingsId} med ytelse " +
                    "kontantstøtte : ${e.message} hendelse={}",
                objectMapper.writeValueAsString(vedtakhendelse),
                e,
            )
            throw e
        }
    }

    private fun erAlleredeHåndtert(vedtakhendelse: VedtakDVH): Boolean {
        return kontantstøtteVedtakService.erAlleredeHåndtert(vedtakhendelse.behandlingsId)
    }

    private fun opprettOppgaveHvisPersonHarLøpendeBarnetilsyn(personIdent: String, behandlingId: String) {
        if (kontantstøtteVedtakService.harLøpendeBarnetilsyn(personIdent)) {
            kontantstøtteVedtakService.opprettVurderKonsekvensOppgaveForBarnetilsyn(
                personIdent = personIdent,
                vurderKonsekvensOppgaveBeskrivelse,
            )
        }
        kontantstøtteVedtakService.lagreKontantstøttehendelse(behandlingId)
    }
}
