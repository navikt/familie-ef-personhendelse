package no.nav.familie.ef.personhendelse.configuration

import no.nav.familie.ef.personhendelse.sikkerhet.TilgangInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val tilgangIntercepter: TilgangInterceptor,
) : WebMvcConfigurer {
    private val exludePatterns =
        listOf(
            "/api/task/**",
            "/api/v2/task/**",
            "/internal/**",
            "/swagger-resources/**",
            "/swagger-resources",
            "/swagger-ui/**",
            "/swagger-ui",
            "/v3/api-docs/**",
            "/v3/api-docs",
        )

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(tilgangIntercepter).excludePathPatterns(exludePatterns)
        super.addInterceptors(registry)
    }
}
