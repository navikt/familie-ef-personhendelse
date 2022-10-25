package no.nav.familie.ef.personhendelse.kafka

import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class PersonhendelseListenerAiven {

    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        id = "familie-ef-personhendelse-aiven",
        groupId = "familie-ef-personhendelse-leesah-1",
        topics = ["pdl.leesah-v1"],
        containerFactory = "kafkaAivenPersonhendelseListenerContainerFactory"
    )
    fun listenAiven(@Payload personhendelse: Personhendelse) {
        logger.info("Leser personhendelse med hendelseId: ${personhendelse.hendelseId} fra Aiven. Gjør ingenting med denne hendelsen.")
    }
}
