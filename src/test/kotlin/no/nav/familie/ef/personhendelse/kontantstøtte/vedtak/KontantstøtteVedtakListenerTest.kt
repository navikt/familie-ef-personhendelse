package no.nav.familie.ef.personhendelse.kontantstøtte.vedtak

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.eksterne.kontrakter.BehandlingType
import no.nav.familie.eksterne.kontrakter.BehandlingÅrsak
import no.nav.familie.eksterne.kontrakter.Kategori
import no.nav.familie.eksterne.kontrakter.PersonDVH
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.familie.kontrakter.felles.jsonMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment
import java.time.ZonedDateTime

class KontantstøtteVedtakListenerTest {
    private val kontantstøtteVedtakServiceMock = mockk<KontantstøtteVedtakService>()
    private val kontantstøtteVedtakListener = KontantstøtteVedtakListener(kontantstøtteVedtakServiceMock)

    private val vedtakDvh =
        VedtakDVH(
            fagsakId = "platea",
            behandlingsId = "utroque",
            tidspunktVedtak = ZonedDateTime.now(),
            person = PersonDVH("123", "", emptyList(), "", 100),
            kategori = Kategori.NASJONAL,
            behandlingType = BehandlingType.REVURDERING,
            utbetalingsperioder = listOf(),
            kompetanseperioder = listOf(),
            funksjonellId = "veri",
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            vilkårResultater = listOf(),
        )

    val vedtakDvhAsJson = jsonMapper.writeValueAsString(vedtakDvh)
    val ack = mockk<Acknowledgment>()

    @BeforeEach
    fun setUp() {
        every { kontantstøtteVedtakServiceMock.lagreKontantstøttehendelse(any()) } just Runs
        every { kontantstøtteVedtakServiceMock.opprettVurderKonsekvensOppgaveForBarnetilsyn(any(), any()) } just Runs
        every { kontantstøtteVedtakServiceMock.harLøpendeBarnetilsyn(any()) } returns true
        every { ack.acknowledge() } just Runs
    }

    @Test
    fun `skal ikke prosessere en hendelse hvis den allerede er lest`() {
        every { kontantstøtteVedtakServiceMock.erAlleredeHåndtert(any()) } returns true

        kontantstøtteVedtakListener.listen(ConsumerRecord("", 1, 1L, "", vedtakDvhAsJson), ack)

        verify(exactly = 0) { kontantstøtteVedtakServiceMock.lagreKontantstøttehendelse(vedtakDvh.behandlingsId) }
        verify(exactly = 0) { kontantstøtteVedtakServiceMock.harLøpendeBarnetilsyn(vedtakDvh.behandlingsId) }
        verify(exactly = 0) { kontantstøtteVedtakServiceMock.opprettVurderKonsekvensOppgaveForBarnetilsyn(any(), any()) }
        verify(exactly = 1) { kontantstøtteVedtakServiceMock.erAlleredeHåndtert(vedtakDvh.behandlingsId) }
    }

    @Test
    fun `skal prosessere nye hendelser`() {
        every { kontantstøtteVedtakServiceMock.erAlleredeHåndtert(any()) } returns false

        kontantstøtteVedtakListener.listen(ConsumerRecord("", 1, 1L, "", vedtakDvhAsJson), ack)

        verify(exactly = 1) { kontantstøtteVedtakServiceMock.lagreKontantstøttehendelse(vedtakDvh.behandlingsId) }
        verify(exactly = 1) { kontantstøtteVedtakServiceMock.harLøpendeBarnetilsyn(vedtakDvh.person.personIdent) }
        verify(exactly = 1) { kontantstøtteVedtakServiceMock.erAlleredeHåndtert(vedtakDvh.behandlingsId) }
        verify(exactly = 1) { kontantstøtteVedtakServiceMock.opprettVurderKonsekvensOppgaveForBarnetilsyn(any(), any()) }
    }
}
