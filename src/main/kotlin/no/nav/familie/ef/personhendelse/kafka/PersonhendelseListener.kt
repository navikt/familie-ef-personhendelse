package no.nav.familie.ef.personhendelse.kafka

import no.nav.familie.ef.personhendelse.dodsfall.DodsfallHandler
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.PartitionOffset
import org.springframework.kafka.annotation.TopicPartition
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component


private const val OPPLYSNINGSTYPE_DODSFALL = "DOEDSFALL_V1"
private const val OPPLYSNINGSTYPE_UTFLYTTING = "UTFLYTTING_V1"

/**
 * Leesah: Livet er en strøm av hendelser
 */
@Component
class PersonhendelseListener(val dodsfallHandler: DodsfallHandler) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private var lestPersonhendelse = false
    private var lestDodsfall = false
    @KafkaListener(
        id = "familie-ef-personhendelse",
        topics = ["aapen-person-pdl-leesah-v1"],
        topicPartitions = [TopicPartition(
            topic = "aapen-person-pdl-leesah-v1",
            partitionOffsets = [PartitionOffset(
                initialOffset = "0", partition = "0"
            )]
        )]
    )
    fun listen(consumerRecord: ConsumerRecord<String, Personhendelse>, ack: Acknowledgment) {
        try {
            val personhendelse = consumerRecord.value()
            if (!lestPersonhendelse) logger.info("Leser personhendelse")
            //logikk her
            if (personhendelse.opplysningstype.erDodsfall()) {
                if (!lestDodsfall) logger.info("Personhendelse med opplysningstype dødsfall")
                lestDodsfall = true
                //dodsfallHandler.handleDodsfallHendelse(personhendelse)
            }

            ack.acknowledge()
            lestPersonhendelse = true
        } catch (e: Exception) {
            //Legg til log
            throw e
        }
    }

    private fun GenericRecord.hentOpplysningstype() =
        get("opplysningstype").toString()

    private fun CharSequence.erDodsfall() =
        this == OPPLYSNINGSTYPE_DODSFALL

    private fun GenericRecord.erUtflytting() =
        hentOpplysningstype() == OPPLYSNINGSTYPE_DODSFALL
}