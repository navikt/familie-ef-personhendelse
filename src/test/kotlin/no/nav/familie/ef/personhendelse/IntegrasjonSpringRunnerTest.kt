package no.nav.familie.ef.personhendelse

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.familie.ef.personhendelse.configuration.DbContainerInitializer
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ApplicationLocal::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@EnableMockOAuth2Server
@ActiveProfiles("integrasjonstest")
abstract class IntegrasjonSpringRunnerTest {
    protected val listAppender = initLoggingEventListAppender()
    protected var loggingEvents: MutableList<ILoggingEvent> = listAppender.list
    protected val restTemplate = TestRestTemplate()

    @Autowired private lateinit var applicationContext: ApplicationContext

    @Autowired private lateinit var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    @LocalServerPort
    private var port: Int? = 0

    @AfterEach
    fun reset() {
        loggingEvents.clear()
        resetDatabase()
        resetWiremockServers()
    }

    private fun resetDatabase() {
        namedParameterJdbcTemplate.update("TRUNCATE TABLE hendelse CASCADE", MapSqlParameterSource())
        namedParameterJdbcTemplate.update("TRUNCATE TABLE efvedtakhendelse CASCADE", MapSqlParameterSource())
        namedParameterJdbcTemplate.update("TRUNCATE TABLE utsattoppgave CASCADE", MapSqlParameterSource())
    }

    private fun resetWiremockServers() {
        applicationContext.getBeansOfType(WireMockServer::class.java).values.forEach(WireMockServer::resetRequests)
    }

    companion object {
        protected fun initLoggingEventListAppender(): ListAppender<ILoggingEvent> {
            val listAppender = ListAppender<ILoggingEvent>()
            listAppender.start()
            return listAppender
        }
    }
}
