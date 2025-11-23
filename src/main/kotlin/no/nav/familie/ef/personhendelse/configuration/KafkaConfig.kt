package no.nav.familie.ef.personhendelse.configuration

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.familie.kafka.KafkaErrorHandler
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties.AckMode

@EnableKafka
@Configuration
class KafkaConfig {
    @Bean
    fun kafkaAivenPersonhendelseListenerContainerFactory(
        properties: KafkaProperties,
        kafkaErrorHandler: KafkaErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<Long, Personhendelse> =
        ConcurrentKafkaListenerContainerFactory<Long, Personhendelse>().apply {
            containerProperties.ackMode = AckMode.MANUAL_IMMEDIATE
            val props = fellesProperties(properties.consumer.buildProperties())
            props[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = true
            props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaAvroDeserializer::class.java
            val consumerFactory = DefaultKafkaConsumerFactory<Long, Personhendelse>(props)

            setConsumerFactory(consumerFactory)
            setCommonErrorHandler(kafkaErrorHandler)
        }

    @Bean
    fun kafkaKontantstøtteVedtakListenerContainerFactory(
        properties: KafkaProperties,
        kafkaErrorHandler: KafkaErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, String> =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            containerProperties.ackMode = AckMode.MANUAL_IMMEDIATE
            val props = fellesProperties(properties.consumer.buildProperties())
            props[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = false
            props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java

            val consumerFactory = DefaultKafkaConsumerFactory<String, String>(props)
            setConsumerFactory(consumerFactory)
            setCommonErrorHandler(kafkaErrorHandler)
        }

    private fun fellesProperties(consumerProperties: Map<String, Any>): MutableMap<String, Any> {
        val props = consumerProperties.toMutableMap()
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 1
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        return props
    }
}
