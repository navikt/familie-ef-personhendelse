package no.nav.familie.ef.personhendelse.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

/**
 * Leesah: Livet er en strøm av hendelser
 */
@Service
class PersonhendelseListener {

    @KafkaListener(id = "familie-ef-personhendelse",
                   topics = ["topic"] )
    fun listen(consumerRecord: ConsumerRecord<String, String>, ack: Acknowledgment) {
        try {
            val hendelseRecord = consumerRecord.value()
            //logikk her
            ack.acknowledge()
        } catch (e: Exception) {
            //Legg til log
            throw e
        }
    }
}