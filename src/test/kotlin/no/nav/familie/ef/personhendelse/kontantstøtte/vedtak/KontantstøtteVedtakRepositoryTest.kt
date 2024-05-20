package no.nav.familie.ef.personhendelse.kontantstøtte.vedtak

import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class KontantstøtteVedtakRepositoryTest : IntegrasjonSpringRunnerTest() {
    @Autowired
    lateinit var kontantstøtteVedtakRepository: KontantstøtteVedtakRepository

    @Test
    fun `skal lagre ned kontantstøttehendelse og sjekke at den finnes`() {
        val behandlingAlleredeHåndtert = UUID.randomUUID().toString()
        val behandlingIkkeHåndtert = UUID.randomUUID().toString()
        kontantstøtteVedtakRepository.lagreKontantstøttevedtak(behandlingAlleredeHåndtert)

        assertThat(
            kontantstøtteVedtakRepository.harAlleredeProsessertKontantstøttevedtak(
                behandlingAlleredeHåndtert,
            ),
        ).isTrue()

        assertThat(
            kontantstøtteVedtakRepository.harAlleredeProsessertKontantstøttevedtak(behandlingIkkeHåndtert),
        ).isFalse()
    }
}
