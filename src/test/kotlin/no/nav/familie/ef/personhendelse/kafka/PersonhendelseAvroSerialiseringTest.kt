package no.nav.familie.ef.personhendelse.kafka

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonhendelseAvroSerialiseringTest {
    private val topic = "pdl.leesah-v1"
    private val registryScope = "personhendelse-serialisering-test"
    private val config =
        mapOf(
            "schema.registry.url" to "mock://$registryScope",
            "specific.avro.reader" to true,
        )
    private val serializer = KafkaAvroSerializer().apply { configure(config, false) }
    private val deserializer = KafkaAvroDeserializer().apply { configure(config, false) }

    @AfterAll
    fun ryddOppMockSchemaRegistry() {
        MockSchemaRegistry.dropScope(registryScope)
    }

    @Test
    fun `skal serialisere og deserialisere en Personhendelse med oppsettet til leesah-konsumenten`() {
        val personhendelse =
            Personhendelse(
                "hendelseId",
                listOf("12345678910"),
                "PDL",
                Instant.now(),
                "FOEDSEL",
                Endringstype.OPPRETTET,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
            )

        val bytes = serializer.serialize(topic, personhendelse)
        val deserialisert = deserializer.deserialize(topic, bytes)

        assertEquals(personhendelse, deserialisert)
    }

    @Test
    fun `skal deserialisere en tombstone-melding til null`() {
        val deserialisert = deserializer.deserialize(topic, null)

        assertNull(deserialisert)
    }
}
