package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.util.JsonFilUtil.Companion.lagInntektResponseForFireMånederFraToJsonsMedEnMåned
import no.nav.familie.ef.personhendelse.util.JsonFilUtil.Companion.lagInntektResponseFraJsonMedEnMåned
import no.nav.familie.ef.personhendelse.util.JsonFilUtil.Companion.readResource
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class VedtakendringUtilTest {
    @Test
    fun `Kun lønnsinntekt og ingen nye vedtak på bruker`() {
        val json: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val oppdatertInntektResponse = lagInntektResponseFraJsonMedEnMåned(json)

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertInntektResponse)).isFalse
    }

    @Test
    fun `Bruker har lønnsinntekt frem til forrige måned`() {
        val jsonMedLønn: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val jsonMedVedtak: String = readResource("inntekt/InntektLoennsinntektTilOffentligYtelseEksempel.json")

        val oppdatertInntektResponse = lagInntektResponseForFireMånederFraToJsonsMedEnMåned(jsonMedLønn, jsonMedVedtak)

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertInntektResponse)).isTrue
    }

    @Test
    fun `Etterbetaling av sykepenger skal ignoreres ved vedtaksendringer`() {
        val jsonMedLønn: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val jsonMedEtterbetaling: String = readResource("inntekt/InntektEtterbetalingSkalIgnoreres.json")

        val oppdatertInntektResponse = lagInntektResponseForFireMånederFraToJsonsMedEnMåned(jsonMedLønn, jsonMedEtterbetaling)

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertInntektResponse)).isFalse
    }

    @Test
    fun `Bruker har fått foreldrepenger i nyeste måned`() {
        val jsonMedLønn: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val jsonMedVedtak: String = readResource("inntekt/InntektMedForeldrepenger.json")

        val oppdatertInntektResponse = lagInntektResponseForFireMånederFraToJsonsMedEnMåned(jsonMedLønn, jsonMedVedtak)

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertInntektResponse)).isTrue
    }

    @Test
    fun `Bruker får overgangsstønad - skal ignoreres`() {
        val jsonMedLønn: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val jsonMedVedtak: String = readResource("inntekt/InntektMedOvergangsstønad.json")

        val oppdatertInntektResponse = lagInntektResponseForFireMånederFraToJsonsMedEnMåned(jsonMedLønn, jsonMedVedtak)

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertInntektResponse)).isFalse
    }
}
