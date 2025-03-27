package no.nav.familie.ef.personhendelse.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class RolleConfig(
    @Value("\${rolle.forvalter}")
    val forvalter: String,
)
