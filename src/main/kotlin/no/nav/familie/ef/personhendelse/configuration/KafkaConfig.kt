package no.nav.familie.ef.personhendelse.configuration

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.familie.kafka.KafkaErrorHandler
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.ssl.SslBundles
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
        sslBundles: ObjectProvider<SslBundles>,
    ): ConcurrentKafkaListenerContainerFactory<Long, Personhendelse> {
        val factory = ConcurrentKafkaListenerContainerFactory<Long, Personhendelse>()
        factory.containerProperties.ackMode = AckMode.MANUAL_IMMEDIATE

        val props = fellesProperties(properties.buildConsumerProperties(sslBundles.getIfAvailable()))
        props[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = true
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaAvroDeserializer::class.java

        factory.consumerFactory = DefaultKafkaConsumerFactory(props)
        factory.setCommonErrorHandler(kafkaErrorHandler)
        return factory
    }

    @Bean
    fun kafkaKontantstøtteVedtakListenerContainerFactory(
        properties: KafkaProperties,
        kafkaErrorHandler: KafkaErrorHandler,
        sslBundles: ObjectProvider<SslBundles>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.containerProperties.ackMode = AckMode.MANUAL_IMMEDIATE

        val props = fellesProperties(properties.buildConsumerProperties(sslBundles.getIfAvailable()))
        props[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = false
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java

        factory.consumerFactory = DefaultKafkaConsumerFactory(props)
        factory.setCommonErrorHandler(kafkaErrorHandler)
        return factory
    }

    private fun fellesProperties(consumerProperties: MutableMap<String, Any>): MutableMap<String, Any> {
        consumerProperties[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        consumerProperties[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 1
        consumerProperties[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        consumerProperties[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        return consumerProperties
    }
}
