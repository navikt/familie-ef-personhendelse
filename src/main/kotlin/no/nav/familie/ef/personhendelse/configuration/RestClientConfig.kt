package no.nav.familie.ef.personhendelse.configuration

import no.nav.familie.felles.tokenklient.entraid.EntraIDRestClientFactory
import no.nav.familie.kontrakter.felles.jsonMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class RestClientConfig(
    private val entraIDRestClientFactory: EntraIDRestClientFactory,
) {
    private fun innloggetBrukerToken(): String? = (SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken)?.token?.tokenValue

    private fun RestClient.medTimeoutOgJsonMapper(
        connectTimeout: Duration = Duration.ofSeconds(2),
        readTimeout: Duration = Duration.ofSeconds(30),
    ): RestClient {
        val requestFactory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(connectTimeout)
                setReadTimeout(readTimeout)
            }
        return mutate()
            .requestFactory(requestFactory)
            .messageConverters { it.add(0, JacksonJsonHttpMessageConverter(jsonMapper)) }
            .build()
    }

    @Bean("integrasjonerRestClient")
    fun integrasjonerRestClient(
        @Value("\${FAMILIE_INTEGRASJONER_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory
            .lagMaskinTilMaskinRestKlient(scope)
            .medTimeoutOgJsonMapper()

    @Bean("pdlRestClient")
    fun pdlRestClient(
        @Value("\${PDL_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory
            .lagMaskinTilMaskinRestKlient(scope)
            .medTimeoutOgJsonMapper()

    @Bean("efSakRestClient")
    fun efSakRestClient(
        @Value("\${EF_SAK_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory
            .lagHybridRestKlient(scope) { innloggetBrukerToken() }
            .medTimeoutOgJsonMapper()

    @Bean("inntektRestClient")
    fun inntektRestClient(
        @Value("\${INNTEKT_SCOPE}") scope: String,
    ): RestClient =
        entraIDRestClientFactory
            .lagHybridRestKlient(scope) { innloggetBrukerToken() }
            .medTimeoutOgJsonMapper()
}
