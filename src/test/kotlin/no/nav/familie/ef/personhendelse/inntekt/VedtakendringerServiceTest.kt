package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.mockk
import no.nav.familie.ef.personhendelse.client.ArbeidsfordelingClient
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.inntekt.vedtak.EfVedtakRepository
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.YearMonth

class VedtakendringerServiceTest {

    private val efVedtakRepository = mockk<EfVedtakRepository>()
    private val inntektClient = mockk<InntektClient>()
    private val oppgaveClient = mockk<OppgaveClient>()
    private val sakClient = mockk<SakClient>()
    private val arbeidsfordelingClient = mockk<ArbeidsfordelingClient>()
    private val inntektsendringerService = mockk<InntektsendringerService>()
    private val vedtakendringer = VedtakendringerService(
        efVedtakRepository,
        inntektClient,
        oppgaveClient,
        sakClient,
        arbeidsfordelingClient,
        inntektsendringerService
    )

    @Test
    fun `Kun lønnsinntekt og ingen nye vedtak på bruker`() {
        val json: String = readResource("inntekt/InntekthistorikkLoennsinntektEksempel.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)

        val nyesteArbeidsInntektInformasjonIEksempelJson = inntektshistorikkResponse.inntektForMåned("2022-01").first().arbeidsInntektInformasjon
        val nestNyesteArbeidsInntektInformasjonIEksempelJson = inntektshistorikkResponse.inntektForMåned("2021-12").first().arbeidsInntektInformasjon

        val oppdatertDatoInntektshistorikkResponse = InntektshistorikkResponse(
            linkedMapOf(
                Pair(YearMonth.now().minusMonths(1).toString(), mapOf(Pair("1", listOf(InntektVersjon(nyesteArbeidsInntektInformasjonIEksempelJson, null, "innleveringstidspunkt", "opplysningspliktig", 1))))),
                Pair(YearMonth.now().minusMonths(2).toString(), mapOf(Pair("1", listOf(InntektVersjon(nestNyesteArbeidsInntektInformasjonIEksempelJson, null, "innleveringstidspunkt", "opplysningspliktig", 1)))))
            )
        )

        Assertions.assertThat(vedtakendringer.harNyeVedtak(oppdatertDatoInntektshistorikkResponse)).isFalse
    }

    @Test
    fun `Etterbetaling av sykepenger skal ignoreres ved vedtaksendringer`() {
        val json: String = readResource("inntekt/InntekthistorikkEtterbetalingSkalIgnoreres.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)

        val nyesteArbeidsInntektInformasjonIEksempelJson = inntektshistorikkResponse.inntektForMåned("2022-02")
        val nestNyesteArbeidsInntektInformasjonIEksempelJson = inntektshistorikkResponse.inntektForMåned("2022-01")

        val oppdatertDatoInntektshistorikkResponse = InntektshistorikkResponse(
            linkedMapOf(
                Pair(YearMonth.now().minusMonths(1).toString(), mapOf(Pair("1", nyesteArbeidsInntektInformasjonIEksempelJson))),
                Pair(YearMonth.now().minusMonths(2).toString(), mapOf(Pair("1", nestNyesteArbeidsInntektInformasjonIEksempelJson)))
            )
        )

        Assertions.assertThat(vedtakendringer.harNyeVedtak(oppdatertDatoInntektshistorikkResponse)).isFalse
    }

    @Test
    fun `Bruker har lønnsinntekt frem til forrige måned`() {
        val json: String = readResource("inntekt/InntekthistorikkLoennsinntektTilOffentligYtelseEksempel.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)

        val nyesteArbeidsInntektInformasjonIEksempelJson = inntektshistorikkResponse.inntektForMåned("2021-12").first().arbeidsInntektInformasjon
        val nestNyesteArbeidsInntektInformasjonIEksempelJson = inntektshistorikkResponse.inntektForMåned("2021-11").first().arbeidsInntektInformasjon

        val oppdatertDatoInntektshistorikkResponse = InntektshistorikkResponse(
            linkedMapOf(
                Pair(YearMonth.now().minusMonths(1).toString(), mapOf(Pair("1", listOf(InntektVersjon(nyesteArbeidsInntektInformasjonIEksempelJson, null, "innleveringstidspunkt", "opplysningspliktig", 1))))),
                Pair(YearMonth.now().minusMonths(2).toString(), mapOf(Pair("1", listOf(InntektVersjon(nestNyesteArbeidsInntektInformasjonIEksempelJson, null, "innleveringstidspunkt", "opplysningspliktig", 1)))))
            )
        )

        Assertions.assertThat(vedtakendringer.harNyeVedtak(oppdatertDatoInntektshistorikkResponse)).isTrue
    }

    @Test
    fun `Bruker har flere utbetalinger på ytelse en måned`() {
        val json: String = readResource("inntekt/InntekthistorikkLoennsinntektTilOffentligYtelseEksempel.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)

        val nyesteArbeidsInntektInformasjonIEksempelJson = inntektshistorikkResponse.inntektForMåned("2021-12").first().arbeidsInntektInformasjon

        val toUtbetalingerSammeYtelse = listOf(nyesteArbeidsInntektInformasjonIEksempelJson.inntektListe!!.first(), nyesteArbeidsInntektInformasjonIEksempelJson.inntektListe?.first()!!.copy(beløp = 10000))

        val oppdatertDatoInntektshistorikkResponse = InntektshistorikkResponse(
            linkedMapOf(
                Pair(YearMonth.now().minusMonths(1).toString(), mapOf(Pair("1", listOf(InntektVersjon(ArbeidsInntekthistorikkInformasjon(null, null, null, toUtbetalingerSammeYtelse), null, "innleveringstidspunkt", "opplysningspliktig", 1))))),
                Pair(
                    YearMonth.now().minusMonths(2).toString(),
                    mapOf(
                        Pair(
                            "1",
                            listOf(InntektVersjon(nyesteArbeidsInntektInformasjonIEksempelJson, null, "innleveringstidspunkt", "opplysningspliktig", 1))
                        )
                    )
                )
            )
        )

        Assertions.assertThat(vedtakendringer.harNyeVedtak(oppdatertDatoInntektshistorikkResponse)).isFalse
    }

    @Test
    fun `Bruker har overgangsstønad`() {
        val json: String = readResource("inntekt/InntekthistorikkMedOvergangsstønad.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)
        val oppdatertInntektshistorikkResponse = oppdatertInntektshistorikkResponseTilNyereDato(inntektshistorikkResponse)

        Assertions.assertThat(vedtakendringer.harNyeVedtak(oppdatertInntektshistorikkResponse)).isFalse
    }

    @Test
    fun `Bruker har flere utbetalinger fra overgangsstønad`() {
        val json: String = readResource("inntekt/InntekthistorikkFlereUtbetalingerOvergangsstønad.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)
        val oppdatertInntektshistorikkResponse = oppdatertInntektshistorikkResponseTilNyereDato(inntektshistorikkResponse)
        Assertions.assertThat(vedtakendringer.harNyeVedtak(oppdatertInntektshistorikkResponse)).isFalse
    }

    @Test
    fun `map stønadtype til behandlingstema`() {
        Assertions.assertThat(StønadType.OVERGANGSSTØNAD.tilBehandlingstemaValue()).isEqualTo(Behandlingstema.Overgangsstønad.value)
    }

    fun oppdatertInntektshistorikkResponseTilNyereDato(inntektshistorikkResponse: InntektshistorikkResponse): InntektshistorikkResponse {
        val keys = inntektshistorikkResponse.aarMaanedHistorikk.keys.sortedBy { YearMonth.parse(it) }
        return InntektshistorikkResponse(
            linkedMapOf(
                Pair(YearMonth.now().minusMonths(1).toString(), inntektshistorikkResponse.inntektEntryForMåned(keys.first())),
                Pair(YearMonth.now().minusMonths(2).toString(), inntektshistorikkResponse.inntektEntryForMåned(keys.last()))
            )
        )
    }

    fun readResource(name: String): String {
        return this::class.java.classLoader.getResource(name)!!.readText(StandardCharsets.UTF_8)
    }
}
