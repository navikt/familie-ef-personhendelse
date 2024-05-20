package no.nav.familie.ef.personhendelse.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.personhendelse.client.SakClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class EfSakClientMock {
    @Bean
    @Profile("mock-efsak")
    @Primary
    fun oppgaveMockRestClient(): SakClient {
        val klient: SakClient = mockk(relaxed = false)

        every {
            klient.harAktivtVedtak(any())
        } returns true

        every {
            klient.hentAlleAktiveIdenterOgForventetInntekt()
        } returns mapOf("1" to 100000, "2" to 150000)

        return klient
    }
}
