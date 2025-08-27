package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.util.JsonFilUtil.Companion.lagInntektsResponseFraJsonMedEnMåned
import no.nav.familie.ef.personhendelse.util.JsonFilUtil.Companion.lagInntektsResponseFraToJsonsMedEnMåned
import no.nav.familie.ef.personhendelse.util.JsonFilUtil.Companion.readResource
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class VedtakendringUtilTest {
    @Test
    fun `Kun lønnsinntekt og ingen nye vedtak på bruker`() {
        val json: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val oppdatertInntektResponse = lagInntektsResponseFraJsonMedEnMåned(json)

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertInntektResponse)).isFalse
    }

    @Test
    fun `Bruker har lønnsinntekt frem til forrige måned`() {
        val jsonMedLønn: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val jsonMedVedtak: String = readResource("inntekt/InntektLoennsinntektTilOffentligYtelseEksempel.json")

        val oppdatertInntektResponse = lagInntektsResponseFraToJsonsMedEnMåned(jsonMedLønn, jsonMedVedtak)

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertInntektResponse)).isTrue
    }

    @Test
    fun `Etterbetaling av sykepenger skal ignoreres ved vedtaksendringer`() {
        val jsonMedLønn: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val jsonMedEtterbetaling: String = readResource("inntekt/InntektEtterbetalingSkalIgnoreres.json")

        val oppdatertInntektResponse = lagInntektsResponseFraToJsonsMedEnMåned(jsonMedLønn, jsonMedEtterbetaling)

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertInntektResponse)).isFalse
    }

    @Test
    fun `Bruker har fått foreldrepenger i nyeste måned`() {
        val jsonMedLønn: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val jsonMedVedtak: String = readResource("inntekt/InntektMedForeldrepenger.json")

        val oppdatertInntektResponse = lagInntektsResponseFraToJsonsMedEnMåned(jsonMedLønn, jsonMedVedtak)

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertInntektResponse)).isTrue
    }

    @Test
    fun `Bruker får overgangsstønad - skal ignoreres`() {
        val jsonMedLønn: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val jsonMedVedtak: String = readResource("inntekt/InntektMedOvergangsstønad.json")

        val oppdatertInntektResponse = lagInntektsResponseFraToJsonsMedEnMåned(jsonMedLønn, jsonMedVedtak)

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertInntektResponse)).isFalse
    }
}
