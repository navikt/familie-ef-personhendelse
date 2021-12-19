package no.nav.familie.ef.personhendelse.configuration

import no.nav.familie.ef.personhendelse.kafka.KafkaErrorHandler
import no.nav.familie.kontrakter.felles.ef.EnsligForsørgerVedtakhendelse
import no.nav.person.pdl.leesah.Personhendelse
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory

@EnableKafka
@Configuration
class KafkaConfig {

    @Bean
    fun kafkaPersonhendelseListenerContainerFactory(properties: KafkaProperties, kafkaErrorHandler: KafkaErrorHandler):
        ConcurrentKafkaListenerContainerFactory<Long, Personhendelse> {
            val factory = ConcurrentKafkaListenerContainerFactory<Long, Personhendelse>()
            factory.consumerFactory = DefaultKafkaConsumerFactory(properties.buildConsumerProperties())
            factory.setErrorHandler(kafkaErrorHandler)
            return factory
        }

    @Bean
    fun kafkaEfVedtakListenerContainerFactory(properties: KafkaProperties, kafkaErrorHandler: KafkaErrorHandler):
            ConcurrentKafkaListenerContainerFactory<Long, EnsligForsørgerVedtakhendelse> {
        val factory = ConcurrentKafkaListenerContainerFactory<Long, EnsligForsørgerVedtakhendelse>()
        factory.consumerFactory = DefaultKafkaConsumerFactory(properties.buildConsumerProperties())
        factory.setErrorHandler(kafkaErrorHandler)
        return factory
    }

}
