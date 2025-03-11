package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.YearMonth

class VedtakendringUtilTest {
    val enMndTilbake = YearMonth.now().minusMonths(1)
    val toMndTilbake = YearMonth.now().minusMonths(2)
    val treMndTilbake = YearMonth.now().minusMonths(3)
    val fireMndTilbake = YearMonth.now().minusMonths(4)

    @Test
    fun `Kun lønnsinntekt og ingen nye vedtak på bruker`() {
        val json: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val inntektResponse = objectMapper.readValue<InntektResponse>(json)

        val arbeidsinntektMåned = inntektResponse.inntektsMåneder.firstOrNull() ?: Assertions.fail("Inntekt mangler")

        val oppdatertInntektResponse =
            inntektResponse.copy(
                inntektsMåneder =
                    listOf(
                        arbeidsinntektMåned.copy(måned = enMndTilbake),
                        arbeidsinntektMåned.copy(måned = toMndTilbake),
                        arbeidsinntektMåned.copy(måned = treMndTilbake),
                        arbeidsinntektMåned.copy(måned = fireMndTilbake),
                    ),
            )

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertInntektResponse)).isFalse
    }

    @Test
    fun `Bruker har lønnsinntekt frem til forrige måned`() {
        val jsonMedLønn: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val inntektResponseMedLønn = objectMapper.readValue<InntektResponse>(jsonMedLønn)
        val json: String = readResource("inntekt/InntektLoennsinntektTilOffentligYtelseEksempel.json")
        val inntektResponseMedVedtak = objectMapper.readValue<InntektResponse>(json)

        val arbeidsinntektMånedMedLønn = inntektResponseMedLønn.inntektsMåneder.firstOrNull() ?: Assertions.fail("Inntekt mangler")
        val arbeidsinntektMedOffentligYtelse = inntektResponseMedVedtak.inntektsMåneder.firstOrNull() ?: Assertions.fail("Inntekt mangler")

        val oppdatertInntektResponse =
            inntektResponseMedLønn.copy(
                inntektsMåneder =
                    listOf(
                        arbeidsinntektMedOffentligYtelse.copy(måned = enMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(måned = toMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(måned = treMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(måned = fireMndTilbake),
                    ),
            )

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertInntektResponse)).isTrue
    }

    @Test
    fun `Etterbetaling av sykepenger skal ignoreres ved vedtaksendringer`() {
        val jsonMedLønn: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val inntektResponseMedLønn = objectMapper.readValue<InntektResponse>(jsonMedLønn)
        val json: String = readResource("inntekt/InntektEtterbetalingSkalIgnoreres.json")
        val inntektResponseMedVedtak = objectMapper.readValue<InntektResponse>(json)

        val arbeidsinntektMånedMedLønn = inntektResponseMedLønn.inntektsMåneder.firstOrNull() ?: Assertions.fail("Inntekt mangler")
        val arbeidsinntektMedEtterbetalingAvSykepenger = inntektResponseMedVedtak.inntektsMåneder.firstOrNull() ?: Assertions.fail("Inntekt mangler")

        val oppdatertInntektResponse =
            inntektResponseMedLønn.copy(
                inntektsMåneder =
                    listOf(
                        arbeidsinntektMedEtterbetalingAvSykepenger.copy(måned = enMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(måned = toMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(måned = treMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(måned = fireMndTilbake),
                    ),
            )

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertInntektResponse)).isFalse
    }

    @Test
    fun `Bruker har fått foreldrepenger i nyeste måned`() {
        val jsonMedLønn: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val inntektResponseMedLønn = objectMapper.readValue<InntektResponse>(jsonMedLønn)
        val json: String = readResource("inntekt/InntektMedForeldrepenger.json")
        val inntektResponseMedVedtak = objectMapper.readValue<InntektResponse>(json)

        val arbeidsinntektMånedMedLønn = inntektResponseMedLønn.inntektsMåneder.firstOrNull() ?: Assertions.fail("Inntekt mangler")
        val arbeidsinntektMedForeldrepenger = inntektResponseMedVedtak.inntektsMåneder.firstOrNull() ?: Assertions.fail("Inntekt mangler")

        val oppdatertInntektResponse =
            inntektResponseMedLønn.copy(
                inntektsMåneder =
                    listOf(
                        arbeidsinntektMedForeldrepenger.copy(måned = enMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(måned = toMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(måned = treMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(måned = fireMndTilbake),
                    ),
            )

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertInntektResponse)).isTrue
    }

    @Test
    fun `Bruker får overgangsstønad - skal ignoreres`() {
        val jsonMedLønn: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val inntektResponseMedLønn = objectMapper.readValue<InntektResponse>(jsonMedLønn)
        val json: String = readResource("inntekt/InntektMedOvergangsstønad.json")
        val inntektResponseMedVedtak = objectMapper.readValue<InntektResponse>(json)

        val arbeidsinntektMånedMedLønn = inntektResponseMedLønn.inntektsMåneder.firstOrNull() ?: Assertions.fail("Inntekt mangler")
        val arbeidsinntektMedOvergangsstønad = inntektResponseMedVedtak.inntektsMåneder.firstOrNull() ?: Assertions.fail("Inntekt mangler")

        val oppdatertInntektResponse =
            inntektResponseMedLønn.copy(
                inntektsMåneder =
                    listOf(
                        arbeidsinntektMedOvergangsstønad.copy(måned = enMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(måned = toMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(måned = treMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(måned = fireMndTilbake),
                    ),
            )

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertInntektResponse)).isFalse
    }

    fun readResource(name: String): String =
        this::class.java.classLoader
            .getResource(name)!!
            .readText(StandardCharsets.UTF_8)
}
