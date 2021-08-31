package no.nav.familie.ef.personhendelse.configuration

import no.nav.familie.ef.personhendelse.kafka.KafkaErrorHandler
import no.nav.person.pdl.leesah.Personhendelse
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import java.time.Duration

@Configuration
class KafkaConfig {

    @Bean
    fun kafkaPersonhendelseListenerContainerFactory(properties: KafkaProperties, kafkaErrorHandler: KafkaErrorHandler)
            : ConcurrentKafkaListenerContainerFactory<Long, Personhendelse> {
        val factory = ConcurrentKafkaListenerContainerFactory<Long, Personhendelse>()
        factory.consumerFactory = DefaultKafkaConsumerFactory(properties.buildConsumerProperties())
        factory.setErrorHandler(kafkaErrorHandler)
        return factory
    }
}