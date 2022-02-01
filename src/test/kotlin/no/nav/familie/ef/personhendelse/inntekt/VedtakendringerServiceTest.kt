package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.mockk
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.inntekt.vedtak.EfVedtakRepository
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class VedtakendringerServiceTest {

    private val efVedtakRepository = mockk<EfVedtakRepository>()
    private val inntektClient = mockk<InntektClient>()
    private val oppgaveClient = mockk<OppgaveClient>()
    private val sakClient = mockk<SakClient>()
    private val inntektsendringerService = mockk<InntektsendringerService>()
    val vedtakendringer = VedtakendringerService(efVedtakRepository, inntektClient, oppgaveClient, sakClient, inntektsendringerService)

    @Test
    fun `Kun lønnsinntekt og ingen nye vedtak på bruker`() {
        val json: String = readResource("inntekt/InntekthistorikkLoennsinntektEksempel.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)

        Assertions.assertThat(vedtakendringer.harNyeVedtak(inntektshistorikkResponse)).isFalse
    }

    fun `Bruker har lønnsinntekt frem til forrige måned`() {
        val json: String = readResource("inntekt/InntekthistorikkLoennsinntektTilOffentligYtelseEksempel.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)

        Assertions.assertThat(vedtakendringer.harNyeVedtak(inntektshistorikkResponse)).isTrue
    }

    fun readResource(name: String): String {
        return this::class.java.classLoader.getResource(name)!!.readText(StandardCharsets.UTF_8)
    }
}
