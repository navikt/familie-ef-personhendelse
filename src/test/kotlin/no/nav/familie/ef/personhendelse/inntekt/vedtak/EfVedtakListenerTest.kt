package no.nav.familie.ef.personhendelse.inntekt.vedtak

import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify
import no.nav.familie.kontrakter.felles.ef.EnsligForsørgerVedtakhendelse
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment

class EfVedtakListenerTest {

    @MockK
    lateinit var efVedtakRepository: EfVedtakRepository

    @MockK(relaxed = true)
    lateinit var ack: Acknowledgment

    private lateinit var efVedtakListener: EfVedtakListener

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        efVedtakListener = EfVedtakListener(efVedtakRepository)
        clearAllMocks()
    }

    @Test
    fun `send inn gyldig consumer record, forvent acknowledged`() {

        every {
            efVedtakRepository.lagreEfVedtakHendelse(any())
        } just Runs

        efVedtakListener.listen(efVedtakhendelse)
        verify(exactly = 1) {
            efVedtakRepository.lagreEfVedtakHendelse(efVedtakhendelse)
        }
    }

    val efVedtakhendelse = EnsligForsørgerVedtakhendelse(1L, "personIdent", StønadType.OVERGANGSSTØNAD)
}
