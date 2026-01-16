package no.nav.familie.ef.personhendelse.config

import org.flywaydb.core.Flyway
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import javax.sql.DataSource

@Configuration
@Profile("local", "integrasjonstest")
class FlywayTestConfig {
    @Bean
    fun flyway(dataSource: DataSource): Flyway =
        Flyway
            .configure()
            .dataSource(dataSource)
            .load()
            .also { it.migrate() }
}
