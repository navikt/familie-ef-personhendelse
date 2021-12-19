package no.nav.familie.ef.personhendelse.personhendelsemapping

import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import javax.sql.DataSource


@Configuration
class DatabaseConfiguration {

    @Bean
    fun namedParameterJdbcTemplate(dataSource: DataSource): NamedParameterJdbcTemplate {
        return NamedParameterJdbcTemplate(dataSource)
    }

    @Bean
    fun cleanMigrateStrategy(): FlywayMigrationStrategy? {
        return FlywayMigrationStrategy { flyway: Flyway ->
            flyway.repair()
            flyway.migrate()
        }
    }
}
