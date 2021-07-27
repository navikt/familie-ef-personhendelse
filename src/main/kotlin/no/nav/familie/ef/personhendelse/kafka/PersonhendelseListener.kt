package no.nav.familie.ef.personhendelse.kafka

import no.nav.familie.ef.personhendelse.dodsfall.DodsfallHandler
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
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

    @KafkaListener(id = "familie-ef-personhendelse",
                   topics = ["aapen-person-pdl-leesah-v1"] )
    fun listen(consumerRecord: ConsumerRecord<String, Personhendelse>, ack: Acknowledgment) {
        try {
            val personhendelse = consumerRecord.value()
            logger.info("Leser personhendelse")
            //logikk her
            if (personhendelse.opplysningstype.erDodsfall()) {
                logger.info("Personhendelse med opplysningstype dødsfall")
                //dodsfallHandler.handleDodsfallHendelse(personhendelse)
            }

            ack.acknowledge()
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