package no.nav.familie.ef.personhendelse.configuration

import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import no.nav.familie.ef.personhendelse.kafka.KafkaErrorHandler
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory

@EnableKafka
@Configuration
class KafkaConfig(
    @Value("\${KAFKA_ONPREM_BOOTSTRAP_SERVERS}")
    private val bootstrapServers: String,
    @Value("\${KAFKA_ONPREM_SCHEMA_REGISTRY_URL}")
    private val schemaRegistryUrl: String,
    @Value("\${SRV_CREDENTIAL_USERNAME}")
    private val username: String,
    @Value("\${SRV_CREDENTIAL_PASSWORD}")
    private val password: String
) {

    @Bean
    fun consumerConfigs(): Map<String, Any> {
        val props: MutableMap<String, Any> = HashMap()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java
        props[ProducerConfig.ACKS_CONFIG] = "all"
        props[ProducerConfig.CLIENT_ID_CONFIG] = username
        props[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = schemaRegistryUrl
        props[SaslConfigs.SASL_JAAS_CONFIG] =
            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";"
        props[SaslConfigs.SASL_MECHANISM] = "PLAIN"
        props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SASL_SSL"

        return props
    }

    @Bean
    fun kafkaPersonhendelseListenerContainerFactory(properties: KafkaProperties, kafkaErrorHandler: KafkaErrorHandler):
        ConcurrentKafkaListenerContainerFactory<Long, Personhendelse> {
            val factory = ConcurrentKafkaListenerContainerFactory<Long, Personhendelse>()
            factory.consumerFactory = DefaultKafkaConsumerFactory(properties.buildConsumerProperties())
            factory.setErrorHandler(kafkaErrorHandler)
            return factory
        }
}
