package no.nav.familie.ef.personhendelse.inntekt.vedtak.ekstern

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.personhendelse.client.pdl.secureLogger
import no.nav.familie.kontrakter.felles.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class EksternVedtakListener(
    val eksternVedtakService: EksternVedtakService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val securelogger = LoggerFactory.getLogger("secureLogger")

    @KafkaListener(
        id = "familie-ef-personhendelse-eksternt-vedtak",
        topics = ["teamfamilie.privat-ensligforsorger-eksterne-vedtak"],
        containerFactory = "kafkaVedtakListenerContainerFactory",
        groupId = "familie-ef-personhendelse-vedtak",
    )
    fun listen(
        consumerRecord: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        val vedtakhendelse = objectMapper.readValue<Vedtakhendelse>(consumerRecord.value())
        try {
            if (eksternVedtakService.mottarEfStønad(vedtakhendelse)) {
                logger.info("Person med aktiv stønad har fått nytt vedtak ${vedtakhendelse.ytelse}")
                secureLogger.info(
                    "Person med fnr: ${vedtakhendelse.fødselsnummer} med aktiv stønad har fått vedtak om " +
                        "${vedtakhendelse.ytelse} fra dato: ${vedtakhendelse.fraDato} og til dato: ${vedtakhendelse.tilDato}",
                )
                // Skal opprette oppgaver automatisk her, men starter med logging for å observere først
            }
            ack.acknowledge()
        } catch (e: Exception) {
            logger.error("Feil ved håndtering av vedtakhendelse: ${vedtakhendelse.ytelse} - se securelogs for mer detaljer")
            securelogger.error(
                "Feil ved håndtering av vedtakhendelse for person ${vedtakhendelse.fødselsnummer} med ytelse ${vedtakhendelse.ytelse} : ${e.message}" +
                    " hendelse={}",
                objectMapper.writeValueAsString(vedtakhendelse),
            )
            throw e
        }
    }
}

data class Vedtakhendelse(
    val fødselsnummer: String,
    val ytelse: Ytelse,
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
)

enum class Ytelse {
    SYKEPENGER,
    FORELDREPENGER,
}
