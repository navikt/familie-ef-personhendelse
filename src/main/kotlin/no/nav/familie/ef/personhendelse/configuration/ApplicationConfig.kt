package no.nav.familie.ef.personhendelse.configuration

import no.nav.familie.http.config.RestTemplateAzure
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.time.temporal.ChronoUnit


@SpringBootConfiguration
@ConfigurationPropertiesScan("no.nav.familie.ef.personhendelse")
@EnableJwtTokenValidation(ignore = ["org.springframework", "springfox.documentation.swagger"])
@Import(RestTemplateAzure::class)
@EnableOAuth2Client(cacheEnabled = true)
class ApplicationConfig {

    /**
     * Overskrever felles sin som bruker proxy, som ikke skal brukes på gcp
     */
    @Bean
    @Primary
    fun restTemplateBuilder(): RestTemplateBuilder {
        val jackson2HttpMessageConverter = MappingJackson2HttpMessageConverter(objectMapper)
        return RestTemplateBuilder()
            .setConnectTimeout(Duration.of(2, ChronoUnit.SECONDS))
            .setReadTimeout(Duration.of(30, ChronoUnit.SECONDS))
            .messageConverters(listOf(jackson2HttpMessageConverter) + RestTemplate().messageConverters)
    }
}