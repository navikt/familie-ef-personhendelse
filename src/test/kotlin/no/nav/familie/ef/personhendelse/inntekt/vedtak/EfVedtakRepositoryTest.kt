package no.nav.familie.ef.personhendelse.inntekt.vedtak

import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.kontrakter.felles.ef.EnsligForsørgerVedtakhendelse
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class EfVedtakRepositoryTest : IntegrasjonSpringRunnerTest() {

    @Autowired
    lateinit var efVedtakRepository: EfVedtakRepository

    @Test
    fun `lagre EnsligForsørgerVedtakhendelse i db`() {

        val efVedtakhendelse = EnsligForsørgerVedtakhendelse(1L, "personIdent", StønadType.OVERGANGSSTØNAD)
        efVedtakRepository.lagreEfVedtakHendelse(efVedtakhendelse)
    }
}
