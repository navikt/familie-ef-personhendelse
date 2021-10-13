package no.nav.familie.ef.personhendelse.kafka

import no.nav.familie.ef.personhendelse.handler.PersonhendelseHandler
import no.nav.familie.log.mdc.MDCConstants
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.util.UUID


/**
 * Leesah: Livet er en strøm av hendelser
 */
@Component
class PersonhendelseListener(
        personhendelseHandlers: List<PersonhendelseHandler>,
        @Value("\${SPRING_PROFILES_ACTIVE}")
        private val env: String
) : ConsumerSeekAware {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val securelogger = LoggerFactory.getLogger("secureLogger")

    private val handlers: Map<String, PersonhendelseHandler> = personhendelseHandlers.associateBy { it.type.hendelsetype }

    init {
        logger.info("Legger til handlers: {}", personhendelseHandlers)
        if (personhendelseHandlers.isEmpty()) {
            error("Finner ikke handlers for personhendelse")
        }
    }

    @KafkaListener(id = "familie-ef-personhendelse",
                   topics = ["aapen-person-pdl-leesah-v1"],
                   containerFactory = "kafkaPersonhendelseListenerContainerFactory")
    fun listen(@Payload personhendelse: Personhendelse) {
        try {
            MDC.put(MDCConstants.MDC_CALL_ID, UUID.randomUUID().toString())
            if (!personhendelse.personidenter.isNullOrEmpty() && !personhendelse.personidenter.first()
                            .isNullOrBlank()) { //Finnes hendelser uten personIdent i dev som følge av opprydding i testdata
                handlers[personhendelse.opplysningstype]?.handle(personhendelse)
            } else {
                if (env != "dev") throw RuntimeException("Hendelse uten personIdent mottatt for hendelseId: ${personhendelse.hendelseId}")
            }
        } catch (e: Exception) {
            logger.error("Feil ved håndtering av personhendelse med hendelseId: ${personhendelse.hendelseId}")
            securelogger.error("Feil ved håndtering av personhendelse med hendelseId ${personhendelse.hendelseId}: ${e.message}")
            throw e
        } finally {
            MDC.remove(MDCConstants.MDC_CALL_ID)
        }
    }

    /* -- Behold denne utkommenterte koden! Kjekt å kunne lese fra start ved behov for debugging i preprod
    override fun onPartitionsAssigned(
        assignments: MutableMap<org.apache.kafka.common.TopicPartition, Long>,
        callback: ConsumerSeekCallback
    ) {
        logger.info("overrided onPartitionsAssigned seekToBeginning")
        assignments.keys.stream()
            .filter { it.topic() == "aapen-person-pdl-leesah-v1" }
            .forEach {
                //callback.seekRelative("aapen-person-pdl-leesah-v1", it.partition(), -100000, false)
                callback.seekToBeginning("aapen-person-pdl-leesah-v1", it.partition())
            }
    }
     */

}