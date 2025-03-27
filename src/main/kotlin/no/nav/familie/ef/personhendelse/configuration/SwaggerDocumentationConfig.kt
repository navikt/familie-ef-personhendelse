package no.nav.familie.ef.personhendelse.configuration

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerDocumentationConfig(
    @Value("\${AUTHORIZATION_URL}")
    val authorizationUrl: String,
    @Value("\${AZUREAD_TOKEN_ENDPOINT_URL}")
    val tokenUrl: String,
    @Value("\${PERSONHENDELSE_SCOPE}")
    val personhendelseScope: String,
) {
    @Bean
    fun openApi(): OpenAPI =
        OpenAPI()
            .components(Components().addSecuritySchemes("bearerAuth", securitySchemes()))
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))

    @Bean
    fun internOpenApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("Alle")
            .packagesToScan("no.nav.familie.ef.personhendelse")
            .build()

    @Bean
    fun forvalterOpenApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("Forvalter")
            .packagesToScan("no.nav.familie.ef.personhendelse.forvaltning")
            .build()

    private fun securitySchemes(): SecurityScheme =
        SecurityScheme()
            .name("bearerAuth")
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .`in`(SecurityScheme.In.HEADER)
            .description("Legg inn STS eller AAD token, uten \"Bearer \" prefiks")
}