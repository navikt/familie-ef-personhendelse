package no.nav.familie.ef.personhendelse.kafka

import no.nav.familie.ef.personhendelse.handler.PersonhendelseService
import no.nav.familie.ef.personhendelse.util.identerUtenAktørId
import no.nav.familie.log.mdc.MDCConstants
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Leesah: Livet er en strøm av hendelser
 */
@Component
@ConditionalOnProperty(name = ["kafka.enabled"], havingValue = "true", matchIfMissing = true)
class PersonhendelseListener(
    @Value("\${SPRING_PROFILES_ACTIVE}")
    private val env: String,
    private val personhendelseService: PersonhendelseService,
) : ConsumerSeekAware {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val securelogger = LoggerFactory.getLogger("secureLogger")

    @KafkaListener(
        id = "familie-ef-personhendelse-aiven",
        groupId = "familie-ef-personhendelse-leesah-2",
        topics = ["pdl.leesah-v1"],
        containerFactory = "kafkaAivenPersonhendelseListenerContainerFactory",
    )
    fun listen(
        @Payload personhendelse: Personhendelse,
        ack: Acknowledgment,
    ) {
        try {
            MDC.put(MDCConstants.MDC_CALL_ID, UUID.randomUUID().toString())
            val personidenter = personhendelse.identerUtenAktørId()
            // Finnes hendelser uten personIdent i dev som følge av opprydding i testdata
            if (!personidenter.firstOrNull().isNullOrBlank()) {
                if (!personhendelseService.harHåndtertHendelse(personhendelse.hendelseId)) {
                    personhendelseService.håndterPersonhendelse(personhendelse)
                } else {
                    logger.info("Har håndtert hendelse: ${personhendelse.hendelseId} - går videre")
                }
            } else {
                if (env != "dev") throw RuntimeException("Hendelse uten personIdent mottatt for hendelseId: ${personhendelse.hendelseId}")
            }
            ack.acknowledge()
        } catch (e: Exception) {
            logger.error("Feil ved håndtering av personhendelse med hendelseId: ${personhendelse.hendelseId}")
            securelogger.error(
                "Feil ved håndtering av personhendelse med hendelseId ${personhendelse.hendelseId}: ${e.message}" +
                    " hendelse=hendelseId=${personhendelse.hendelseId}, personidenter=${personhendelse.personidenter}, " +
                    "opplysningstype=${personhendelse.opplysningstype}, endringstype=${personhendelse.endringstype}",
            )
            throw e
        } finally {
            MDC.remove(MDCConstants.MDC_CALL_ID)
        }
    }

    /*
    // listen()-metoden må implementere interfacet ConsumerSeekAware for at dette skal virke
    override fun onPartitionsAssigned(
        assignments: MutableMap<org.apache.kafka.common.TopicPartition, Long>,
        callback: ConsumerSeekAware.ConsumerSeekCallback
    ) {
        logger.info("overrided onPartitionsAssigned seekToBeginning")
        assignments.keys.stream()
            .filter { it.topic() == "leesah-v1" }
            .forEach {
                //callback.seekToEnd("leesah-v1", it.partition())
                callback.seekToBeginning("leesah-v1", it.partition())
            }
    }
     */
}
