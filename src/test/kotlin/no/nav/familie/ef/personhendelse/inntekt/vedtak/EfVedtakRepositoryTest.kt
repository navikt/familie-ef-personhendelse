package no.nav.familie.ef.personhendelse.inntekt.vedtak

import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.kontrakter.felles.ef.EnsligForsørgerVedtakhendelse
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class EfVedtakRepositoryTest : IntegrasjonSpringRunnerTest() {

    @Autowired
    lateinit var efVedtakRepository: EfVedtakRepository

    @Test
    fun `lagre og hent EnsligForsørgerVedtakshendelse i db`() {

        val efVedtakshendelse = EnsligForsørgerVedtakhendelse(1L, "personIdent", StønadType.OVERGANGSSTØNAD)
        efVedtakRepository.lagreEfVedtakshendelse(efVedtakshendelse)

        val vedtakshendelse = efVedtakRepository.hentEfVedtakHendelse("personIdent")
        Assertions.assertThat(vedtakshendelse).isNotNull
        Assertions.assertThat(1L).isEqualTo(vedtakshendelse?.behandlingId)
        Assertions.assertThat(StønadType.OVERGANGSSTØNAD).isEqualTo(vedtakshendelse?.stønadType)
    }
}
