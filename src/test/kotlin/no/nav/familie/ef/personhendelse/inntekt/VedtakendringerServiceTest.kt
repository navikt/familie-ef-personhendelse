package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.mockk
import no.nav.familie.ef.personhendelse.client.ArbeidsfordelingClient
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.YearMonth

class VedtakendringerServiceTest {
    private val efVedtakRepository = mockk<EfVedtakRepository>()
    private val inntektClient = mockk<InntektClient>()
    private val oppgaveClient = mockk<OppgaveClient>()
    private val sakClient = mockk<SakClient>()
    private val arbeidsfordelingClient = mockk<ArbeidsfordelingClient>()
    private val inntektsendringerService = mockk<InntektsendringerService>()
    private val vedtakendringer =
        VedtakendringerService(
            efVedtakRepository,
            inntektClient,
            oppgaveClient,
            sakClient,
            arbeidsfordelingClient,
            inntektsendringerService,
        )

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

        Assertions.assertThat(vedtakendringer.harNyeVedtak(oppdatertDatoHentInntektListeResponse)).isFalse
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

        Assertions.assertThat(vedtakendringer.harNyeVedtak(oppdatertDatoHentInntektListeResponse)).isTrue
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

        Assertions.assertThat(vedtakendringer.harNyeVedtak(oppdatertDatoHentInntektListeResponse)).isFalse
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

        Assertions.assertThat(vedtakendringer.harNyeVedtak(oppdatertDatoHentInntektListeResponse)).isTrue
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

        Assertions.assertThat(vedtakendringer.harNyeVedtak(oppdatertDatoHentInntektListeResponse)).isFalse
    }

    @Test
    fun `map stønadtype til behandlingstema`() {
        Assertions.assertThat(StønadType.OVERGANGSSTØNAD.tilBehandlingstemaValue()).isEqualTo(Behandlingstema.Overgangsstønad.value)
    }

    @Test
    fun `lagOppgavetekstForInntektsendring - sjekk tusenskille på feiltubetalingsbeløp og norsk format på år-måned`() {
        val oppgavetekst =
            vedtakendringer.lagOppgavetekstForInntektsendring(
                InntektOgVedtakEndring(
                    "1",
                    false,
                    LocalDateTime.of(2023, 11, 8, 5, 0),
                    BeregningResultat(1, 1, 1),
                    BeregningResultat(2, 2, 2),
                    BeregningResultat(3, 3, 3),
                    BeregningResultat(4, 4, 40000),
                    null,
                ),
            )

        Assertions.assertThat(oppgavetekst.contains("Beregnet feilutbetaling i uttrekksperioden: 40 006 kroner "))
        Assertions.assertThat(oppgavetekst.contains("FOM 06.2023 - TOM 10.2023"))
    }

    fun readResource(name: String): String =
        this::class.java.classLoader
            .getResource(name)!!
            .readText(StandardCharsets.UTF_8)
}
