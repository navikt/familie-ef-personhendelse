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
        val inntektResponse = objectMapper.readValue<HentInntektListeResponse>(json)

        val arbeidsinntektMåned =
            inntektResponse.arbeidsinntektMåned
                ?.first() ?: Assertions.fail("Inntekt mangler")

        val oppdatertDatoHentInntektListeResponse =
            inntektResponse.copy(
                arbeidsinntektMåned =
                    listOf(
                        arbeidsinntektMåned.copy(årMåned = enMndTilbake),
                        arbeidsinntektMåned.copy(årMåned = toMndTilbake),
                        arbeidsinntektMåned.copy(årMåned = treMndTilbake),
                        arbeidsinntektMåned.copy(årMåned = fireMndTilbake),
                    ),
            )

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertDatoHentInntektListeResponse)).isFalse
    }

    @Test
    fun `Bruker har lønnsinntekt frem til forrige måned`() {
        val jsonMedLønn: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val inntektResponseMedLønn = objectMapper.readValue<HentInntektListeResponse>(jsonMedLønn)
        val json: String = readResource("inntekt/InntektLoennsinntektTilOffentligYtelseEksempel.json")
        val inntektResponseMedVedtak = objectMapper.readValue<HentInntektListeResponse>(json)

        val arbeidsinntektMånedMedLønn =
            inntektResponseMedLønn.arbeidsinntektMåned
                ?.first() ?: Assertions.fail("Inntekt mangler")

        val arbeidsinntektMedOffentligYtelse =
            inntektResponseMedVedtak.arbeidsinntektMåned
                ?.first() ?: Assertions.fail("Inntekt mangler")

        val oppdatertDatoHentInntektListeResponse =
            inntektResponseMedLønn.copy(
                arbeidsinntektMåned =
                    listOf(
                        arbeidsinntektMedOffentligYtelse.copy(årMåned = enMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(årMåned = toMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(årMåned = treMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(årMåned = fireMndTilbake),
                    ),
            )

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertDatoHentInntektListeResponse)).isTrue
    }

    @Test
    fun `Etterbetaling av sykepenger skal ignoreres ved vedtaksendringer`() {
        val jsonMedLønn: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val inntektResponseMedLønn = objectMapper.readValue<HentInntektListeResponse>(jsonMedLønn)
        val json: String = readResource("inntekt/InntektEtterbetalingSkalIgnoreres.json")
        val inntektResponseMedVedtak = objectMapper.readValue<HentInntektListeResponse>(json)

        val arbeidsinntektMånedMedLønn =
            inntektResponseMedLønn.arbeidsinntektMåned
                ?.first() ?: Assertions.fail("Inntekt mangler")

        val arbeidsinntektMedEtterbetalingAvSykepenger =
            inntektResponseMedVedtak.arbeidsinntektMåned
                ?.first() ?: Assertions.fail("Inntekt mangler")

        val oppdatertDatoHentInntektListeResponse =
            inntektResponseMedLønn.copy(
                arbeidsinntektMåned =
                    listOf(
                        arbeidsinntektMedEtterbetalingAvSykepenger.copy(årMåned = enMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(årMåned = toMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(årMåned = treMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(årMåned = fireMndTilbake),
                    ),
            )

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertDatoHentInntektListeResponse)).isFalse
    }

    @Test
    fun `Bruker har fått foreldrepenger i nyeste måned`() {
        val jsonMedLønn: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val inntektResponseMedLønn = objectMapper.readValue<HentInntektListeResponse>(jsonMedLønn)
        val json: String = readResource("inntekt/InntektMedForeldrepenger.json")
        val inntektResponseMedVedtak = objectMapper.readValue<HentInntektListeResponse>(json)

        val arbeidsinntektMånedMedLønn =
            inntektResponseMedLønn.arbeidsinntektMåned
                ?.first() ?: Assertions.fail("Inntekt mangler")

        val arbeidsinntektMedForeldrepenger =
            inntektResponseMedVedtak.arbeidsinntektMåned
                ?.first() ?: Assertions.fail("Inntekt mangler")

        val oppdatertDatoHentInntektListeResponse =
            inntektResponseMedLønn.copy(
                arbeidsinntektMåned =
                    listOf(
                        arbeidsinntektMedForeldrepenger.copy(årMåned = enMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(årMåned = toMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(årMåned = treMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(årMåned = fireMndTilbake),
                    ),
            )

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertDatoHentInntektListeResponse)).isTrue
    }

    @Test
    fun `Bruker får overgangsstønad - skal ignoreres`() {
        val jsonMedLønn: String = readResource("inntekt/InntektLoennsinntektEksempel.json")
        val inntektResponseMedLønn = objectMapper.readValue<HentInntektListeResponse>(jsonMedLønn)
        val json: String = readResource("inntekt/InntektMedOvergangsstønad.json")
        val inntektResponseMedVedtak = objectMapper.readValue<HentInntektListeResponse>(json)

        val arbeidsinntektMånedMedLønn =
            inntektResponseMedLønn.arbeidsinntektMåned
                ?.first() ?: Assertions.fail("Inntekt mangler")

        val arbeidsinntektMedOvergangsstønad =
            inntektResponseMedVedtak.arbeidsinntektMåned
                ?.first() ?: Assertions.fail("Inntekt mangler")

        val oppdatertDatoHentInntektListeResponse =
            inntektResponseMedLønn.copy(
                arbeidsinntektMåned =
                    listOf(
                        arbeidsinntektMedOvergangsstønad.copy(årMåned = enMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(årMåned = toMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(årMåned = treMndTilbake),
                        arbeidsinntektMånedMedLønn.copy(årMåned = fireMndTilbake),
                    ),
            )

        Assertions.assertThat(VedtakendringerUtil.harNyeVedtak(oppdatertDatoHentInntektListeResponse)).isFalse
    }

    fun readResource(name: String): String =
        this::class.java.classLoader
            .getResource(name)!!
            .readText(StandardCharsets.UTF_8)
}
