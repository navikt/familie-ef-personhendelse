package no.nav.familie.ef.personhendelse.configuration

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.familie.kafka.KafkaErrorHandler
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG
import org.apache.kafka.common.config.SaslConfigs
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

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
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = KafkaAvroDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaAvroDeserializer::class.java
        props[ConsumerConfig.CLIENT_ID_CONFIG] = username
        props[KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = schemaRegistryUrl
        props[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = true
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
        factory.consumerFactory = DefaultKafkaConsumerFactory(consumerConfigs())
        factory.setCommonErrorHandler(kafkaErrorHandler)
        return factory
    }

    @Bean
    fun kafkaAivenPersonhendelseListenerContainerFactory(properties: KafkaProperties, kafkaErrorHandler: KafkaErrorHandler):
        ConcurrentKafkaListenerContainerFactory<Long, Personhendelse> {
        properties.properties[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = "true"
        val factory = ConcurrentKafkaListenerContainerFactory<Long, Personhendelse>()
        factory.consumerFactory = DefaultKafkaConsumerFactory(properties.buildConsumerProperties())
        factory.setCommonErrorHandler(kafkaErrorHandler)
        return factory
    }

    @Bean
    fun kafkaVedtakListenerContainerFactory(properties: KafkaProperties, kafkaErrorHandler: KafkaErrorHandler):
        ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        properties.properties[VALUE_DESERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringDeserializer"
        properties.properties[KEY_DESERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringDeserializer"
        factory.consumerFactory = DefaultKafkaConsumerFactory(properties.buildConsumerProperties())
        factory.setCommonErrorHandler(kafkaErrorHandler)
        return factory
    }
}
