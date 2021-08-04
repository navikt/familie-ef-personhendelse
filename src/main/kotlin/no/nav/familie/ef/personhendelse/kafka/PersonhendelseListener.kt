package no.nav.familie.ef.personhendelse.kafka

import no.nav.familie.ef.personhendelse.dodsfall.DodsfallHandler
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.avro.generic.GenericRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.PartitionOffset
import org.springframework.kafka.annotation.TopicPartition
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.kafka.listener.ConsumerSeekAware.ConsumerSeekCallback
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component


private const val OPPLYSNINGSTYPE_DODSFALL = "DOEDSFALL_V1"
private const val OPPLYSNINGSTYPE_UTFLYTTING = "UTFLYTTING_V1"

/**
 * Leesah: Livet er en strøm av hendelser
 */
@Component
class PersonhendelseListener(val dodsfallHandler: DodsfallHandler) : ConsumerSeekAware {

    private val logger = LoggerFactory.getLogger(javaClass)
    private var lestPersonhendelse = false
    private var lestDodsfall = false

    @KafkaListener(id = "familie-ef-personhendelse", topics = ["aapen-person-pdl-leesah-v1"])
    fun listen(@Payload personhendelse: Personhendelse) {
        try {

            if (!lestPersonhendelse) logger.info("Leser personhendelse")
            //logikk her
            if (personhendelse.opplysningstype.erDodsfall()) {
                if (!lestDodsfall) logger.info("Personhendelse med opplysningstype dødsfall ${personhendelse.hendelseId}")
                //dodsfallHandler.handleDodsfallHendelse(personhendelse)
            }

            lestPersonhendelse = true
        } catch (e: Exception) {
            //Legg til log
            throw e
        }
    }

    override fun onPartitionsAssigned(
        assignments: MutableMap<org.apache.kafka.common.TopicPartition, Long>,
        callback: ConsumerSeekCallback
    ) {
        logger.info("overrided onPartitionsAssigned seekToBeginning")
        assignments.keys.stream()
            .filter { it.topic() == "aapen-person-pdl-leesah-v1" }
            .forEach {
                callback.seekRelative("aapen-person-pdl-leesah-v1", it.partition(), -100000, false)
            }
    }

    private fun GenericRecord.hentOpplysningstype() =
        get("opplysningstype").toString()

    private fun CharSequence.erDodsfall() =
        this == OPPLYSNINGSTYPE_DODSFALL

    private fun GenericRecord.erUtflytting() =
        hentOpplysningstype() == OPPLYSNINGSTYPE_DODSFALL
}