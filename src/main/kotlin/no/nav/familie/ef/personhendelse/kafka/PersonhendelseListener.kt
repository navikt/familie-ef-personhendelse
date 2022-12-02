package no.nav.familie.ef.personhendelse.kafka

import no.nav.familie.ef.personhendelse.handler.PersonhendelseService
import no.nav.familie.ef.personhendelse.util.identerUtenAktørId
import no.nav.familie.kontrakter.felles.objectMapper
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
    @Value("\${SPRING_PROFILES_ACTIVE}")
    private val env: String,
    private val personhendelseService: PersonhendelseService
) : ConsumerSeekAware {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val securelogger = LoggerFactory.getLogger("secureLogger")

    @KafkaListener(
        id = "familie-ef-personhendelse-aiven",
        groupId = "familie-ef-personhendelse-leesah-1",
        topics = ["pdl.leesah-v1"],
        containerFactory = "kafkaAivenPersonhendelseListenerContainerFactory"
    )
    fun listen(@Payload personhendelse: Personhendelse) {
        try {
            MDC.put(MDCConstants.MDC_CALL_ID, UUID.randomUUID().toString())
            val personidenter = personhendelse.identerUtenAktørId()
            // Finnes hendelser uten personIdent i dev som følge av opprydding i testdata
            logger.info(
                "Leser personhendelse med hendelseId=${personhendelse.hendelseId} " +
                    "tidligereHendelseId=${personhendelse.tidligereHendelseId}"
            )
            if (!personidenter.firstOrNull().isNullOrBlank() &&
                !personhendelseService.harHåndtertHendelse(personhendelse.hendelseId)
            ) {
                personhendelseService.håndterPersonhendelse(personhendelse)
            } else {
                if (env != "dev") throw RuntimeException("Hendelse uten personIdent mottatt for hendelseId: ${personhendelse.hendelseId}")
            }
        } catch (e: Exception) {
            logger.error("Feil ved håndtering av personhendelse med hendelseId: ${personhendelse.hendelseId}")
            securelogger.error(
                "Feil ved håndtering av personhendelse med hendelseId ${personhendelse.hendelseId}: ${e.message}" +
                    " hendelse={}",
                objectMapper.writeValueAsString(personhendelse)
            )
            throw e
        } finally {
            MDC.remove(MDCConstants.MDC_CALL_ID)
        }
    }

    /* -- Behold denne utkommenterte koden! Kjekt å kunne lese fra start ved behov for debugging i preprod
    override fun onPartitionsAssigned(
        assignments: MutableMap<org.apache.kafka.common.TopicPartition, Long>,
        callback: ConsumerSeekAware.ConsumerSeekCallback
    ) {
        logger.info("overrided onPartitionsAssigned seekToBeginning")
        assignments.keys.stream()
            .filter { it.topic() == "aapen-person-pdl-leesah-v1" }
            .forEach {
                callback.seekToEnd("aapen-person-pdl-leesah-v1", it.partition())
                // callback.seekToBeginning("aapen-person-pdl-leesah-v1", it.partition())
            }
    }
     */
}
