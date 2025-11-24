package no.nav.familie.ef.personhendelse.config

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.mockk.mockk
import no.nav.familie.kafka.KafkaErrorHandler
import no.nav.person.pdl.leesah.Personhendelse
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties.AckMode

@TestConfiguration
@Profile("disable-kafka")
class NoKafkaConfig {
    // Oppretter dummy KafkaTemplate slik at autowiring ikke feiler
    @Bean
    fun kafkaTemplate(): KafkaTemplate<Any, Any> = mockk(relaxed = true)

    // Oppretter dummy ConsumerFactory
    @Bean
    fun consumerFactory(): ConsumerFactory<Any, Any> = mockk(relaxed = true)

    // Listener registry dummy
    @Bean
    fun kafkaListenerEndpointRegistry(): KafkaListenerEndpointRegistry = KafkaListenerEndpointRegistry()

    @Bean
    fun kafkaAivenPersonhendelseListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<Long, Personhendelse> = mockk(relaxed = true)

    @Bean
    fun kafkaKontantstøtteVedtakListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> = mockk(relaxed = true)
}
