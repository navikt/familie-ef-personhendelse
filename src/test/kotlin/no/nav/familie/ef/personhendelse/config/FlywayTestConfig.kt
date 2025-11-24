package no.nav.familie.ef.personhendelse.config

import org.flywaydb.core.Flyway
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import javax.sql.DataSource

@TestConfiguration
class FlywayTestConfig {
    @Bean
    fun flyway(dataSource: DataSource): Flyway =
        Flyway
            .configure()
            .dataSource(dataSource)
            .load()
            .also { it.migrate() }
}
