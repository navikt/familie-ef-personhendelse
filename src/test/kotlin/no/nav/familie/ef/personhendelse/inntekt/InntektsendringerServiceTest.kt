package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.inntekt.vedtak.EfVedtakRepository
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.YearMonth

class InntektsendringerServiceTest {

    private val efVedtakRepository = mockk<EfVedtakRepository>()
    private val inntektClient = mockk<InntektClient>()
    private val oppgaveClient = mockk<OppgaveClient>()
    private val sakClient = mockk<SakClient>()

    val inntektsendringer = InntektsendringerService(efVedtakRepository, inntektClient, oppgaveClient, sakClient)

    @BeforeEach
    internal fun setUp() {
        val forventetLønnsinntekt = 420000 // 35k pr mnd i eksempel json-fil
        every { sakClient.inntektForEksternId(1) } returns forventetLønnsinntekt
        every { sakClient.inntektForEksternId(2) } returns (forventetLønnsinntekt * 0.9).toInt()
        every { sakClient.inntektForEksternId(3) } returns (forventetLønnsinntekt * 0.91).toInt()
    }

    @Test
    fun `Har endret inntekt med mer enn 10 prosent i forhold til forventet inntekt`() {
        val json: String = readResource("inntekt/InntekthistorikkLoennsinntektEksempel.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)

        val nyesteArbeidsInntektInformasjonIEksempelJson = inntektshistorikkResponse.inntektForMåned("2022-01")?.first()?.arbeidsInntektInformasjon!!
        val nestNyesteArbeidsInntektInformasjonIEksempelJson = inntektshistorikkResponse.inntektForMåned("2021-12")?.first()?.arbeidsInntektInformasjon!!

        val oppdatertDatoInntektshistorikkResponse = InntektshistorikkResponse(
            linkedMapOf(
                Pair(YearMonth.now().minusMonths(1).toString(), mapOf(Pair("928497704", listOf(InntektVersjon(nyesteArbeidsInntektInformasjonIEksempelJson, null, "innleveringstidspunkt", "opplysningspliktig", 1))))),
                Pair(YearMonth.now().minusMonths(2).toString(), mapOf(Pair("928497704", listOf(InntektVersjon(nestNyesteArbeidsInntektInformasjonIEksempelJson, null, "innleveringstidspunkt", "opplysningspliktig", 1)))))
            )
        )

        Assertions.assertThat(inntektsendringer.harEndretInntekt(oppdatertDatoInntektshistorikkResponse, 1)).isFalse
        Assertions.assertThat(inntektsendringer.harEndretInntekt(oppdatertDatoInntektshistorikkResponse, 2)).isTrue
        Assertions.assertThat(inntektsendringer.harEndretInntekt(oppdatertDatoInntektshistorikkResponse, 3)).isFalse
    }

    @Test
    fun `Har mottatt mer i offentlige ytelser`() {
        val json: String = readResource("inntekt/InntekthistorikkYtelseFraOffentligeEksempel.json")
        val inntektshistorikkResponse = objectMapper.readValue<InntektshistorikkResponse>(json)
        val nyesteArbeidsInntektInformasjonIEksempelJson = inntektshistorikkResponse.inntektForMåned("2022-01")?.first()?.arbeidsInntektInformasjon!!
        val nestNyesteArbeidsInntektInformasjonIEksempelJson = inntektshistorikkResponse.inntektForMåned("2021-12")?.first()?.arbeidsInntektInformasjon!!

        val oppdatertDatoInntektshistorikkResponse = InntektshistorikkResponse(
            linkedMapOf(
                Pair(YearMonth.now().minusMonths(1).toString(), mapOf(Pair("928497704", listOf(InntektVersjon(nyesteArbeidsInntektInformasjonIEksempelJson, null, "innleveringstidspunkt", "opplysningspliktig", 1))))),
                Pair(YearMonth.now().minusMonths(2).toString(), mapOf(Pair("928497704", listOf(InntektVersjon(nestNyesteArbeidsInntektInformasjonIEksempelJson, null, "innleveringstidspunkt", "opplysningspliktig", 1)))))
            )
        )

        Assertions.assertThat(inntektsendringer.harEndretInntekt(oppdatertDatoInntektshistorikkResponse, 1)).isTrue
    }

    fun readResource(name: String): String {
        return this::class.java.classLoader.getResource(name)!!.readText(StandardCharsets.UTF_8)
    }
}
