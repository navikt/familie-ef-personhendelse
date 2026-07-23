package no.nav.familie.ef.personhendelse.configuration

import no.nav.familie.kafka.KafkaErrorHandler
import no.nav.familie.log.NavSystemtype
import no.nav.familie.log.filter.LogFilter
import no.nav.familie.log.filter.RequestTimeFilter
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.familie.sikkerhet.context.FamilieFellesSpringSecurityKonfigurasjon
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.resilience.annotation.EnableResilientMethods
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import tools.jackson.module.kotlin.KotlinModule

@SpringBootConfiguration
@ConfigurationPropertiesScan("no.nav.familie.ef.personhendelse")
@ComponentScan(
    "no.nav.familie.prosessering",
    "no.nav.familie.felles.tokenklient",
)
@EnableScheduling
@Import(
    KafkaErrorHandler::class,
    FamilieFellesSpringSecurityKonfigurasjon::class,
)
@EnableResilientMethods
class ApplicationConfig {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun kotlinModule(): KotlinModule = KotlinModule.Builder().build()

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> =
        FilterRegistrationBean(LogFilter(systemtype = NavSystemtype.NAV_INTEGRASJON)).apply {
            logger.info("Registering LogFilter filter")
            order = 1
        }

    @Bean
    fun requestTimeFilter(): FilterRegistrationBean<RequestTimeFilter> =
        FilterRegistrationBean(RequestTimeFilter()).apply {
            logger.info("Registering RequestTimeFilter filter")
            order = 2
        }

    @Bean
    fun prosesseringInfoProvider(
        @Value("\${prosessering.rolle}") prosesseringRolle: String,
    ) = object : ProsesseringInfoProvider {
        override fun hentBrukernavn(): String {
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication is JwtAuthenticationToken) {
                return authentication.token.getClaimAsString("preferred_username")
                    ?: error("preferred_username claim mangler i token")
            }
            error("Finner ikke brukernavn i security context")
        }

        override fun harTilgang(): Boolean {
            val authentication = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
            val grupper = authentication?.token?.getClaimAsStringList("groups")?.toSet() ?: emptySet()
            return grupper.contains(prosesseringRolle)
        }
    }
}
