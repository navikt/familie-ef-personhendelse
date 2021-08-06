package no.nav.familie.ef.personhendelse.kafka

import no.nav.familie.ef.personhendelse.dodsfall.DodsfallHandler
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.avro.generic.GenericRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.kafka.listener.ConsumerSeekAware.ConsumerSeekCallback
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component


private const val OPPLYSNINGSTYPE_DODSFALL = "DOEDSFALL_V1"
private const val OPPLYSNINGSTYPE_UTFLYTTING = "UTFLYTTING_V1"

/**
 * Leesah: Livet er en strøm av hendelser
 */
@Component
class PersonhendelseListener(
    val dodsfallHandler: DodsfallHandler,
    @Value("\${SPRING_PROFILES_ACTIVE}")
    private val env: String
) : ConsumerSeekAware {

    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(id = "familie-ef-personhendelse", topics = ["aapen-person-pdl-leesah-v1"])
    fun listen(@Payload personhendelse: Personhendelse) {
        try {
            if (!personhendelse.personidenter.isNullOrEmpty()) { //Finnes hendelser uten personIdent i dev som følge av opprydding i testdata
                if (personhendelse.opplysningstype.erDodsfall()) {
                    dodsfallHandler.handleDodsfallHendelse(personhendelse)
                }
            } else {
                if (env != "dev") throw RuntimeException("Hendelse uten personIdent mottatt for hendelseId: ${personhendelse.hendelseId}")
            }
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
                //callback.seekRelative("aapen-person-pdl-leesah-v1", it.partition(), -100000, false)
                callback.seekToBeginning("aapen-person-pdl-leesah-v1", it.partition())
            }
    }

    private fun GenericRecord.hentOpplysningstype() =
        get("opplysningstype").toString()

    private fun CharSequence.erDodsfall() =
        this.toString() == OPPLYSNINGSTYPE_DODSFALL

    private fun GenericRecord.erUtflytting() =
        hentOpplysningstype() == OPPLYSNINGSTYPE_DODSFALL
}