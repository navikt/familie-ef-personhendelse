package no.nav.familie.ef.personhendelse.kafka

import no.nav.familie.ef.personhendelse.handler.DodsfallHandler
import no.nav.familie.ef.personhendelse.handler.SivilstandHandler
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.avro.generic.GenericRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component


private const val OPPLYSNINGSTYPE_DODSFALL = "DOEDSFALL_V1"
private const val OPPLYSNINGSTYPE_SIVILSTAND = "SIVILSTAND_V1"

/**
 * Leesah: Livet er en strøm av hendelser
 */
@Component
class PersonhendelseListener(
    val dodsfallHandler: DodsfallHandler,
    val sivilstandHandler: SivilstandHandler,
    @Value("\${SPRING_PROFILES_ACTIVE}")
    private val env: String
) : ConsumerSeekAware {

    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(id = "familie-ef-personhendelse", topics = ["aapen-person-pdl-leesah-v1"])
    fun listen(@Payload personhendelse: Personhendelse) {
        try {
            if (!personhendelse.personidenter.isNullOrEmpty() && !personhendelse.personidenter.first().isNullOrBlank()) { //Finnes hendelser uten personIdent i dev som følge av opprydding i testdata
                when (personhendelse.opplysningstype.toString()) {
                    OPPLYSNINGSTYPE_DODSFALL -> dodsfallHandler.handleDodsfall(personhendelse)
                    OPPLYSNINGSTYPE_SIVILSTAND -> sivilstandHandler.handleSivilstand(personhendelse)
                }
            } else {
                if (env != "dev") throw RuntimeException("Hendelse uten personIdent mottatt for hendelseId: ${personhendelse.hendelseId}")
            }
        } catch (e: Exception) {
            logger.error("Feil ved håndtering av personhendelse med hendelseId: ${personhendelse.hendelseId}: ${e.message}")
            throw e
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

    private fun GenericRecord.hentOpplysningstype() =
        get("opplysningstype").toString()

    private fun CharSequence.erDodsfall() =
        this.toString() == OPPLYSNINGSTYPE_DODSFALL

    private fun CharSequence.erSivilstand() =
        this.toString() == OPPLYSNINGSTYPE_SIVILSTAND
}