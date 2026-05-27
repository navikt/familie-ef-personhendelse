package no.nav.familie.ef.personhendelse.util

import no.nav.security.mock.oauth2.MockOAuth2Server
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

class MockOAuth2ServerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val issuerUri = mockOAuth2Server.issuerUrl("azuread").toString()
        System.setProperty("AZURE_OPENID_CONFIG_ISSUER", issuerUri)
        System.setProperty("AZURE_APP_CLIENT_ID", "aud-localhost")
    }

    companion object {
        val mockOAuth2Server: MockOAuth2Server by lazy {
            MockOAuth2Server().also { it.start() }
        }
    }
}
