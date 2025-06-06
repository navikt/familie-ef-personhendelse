package no.nav.familie.ef.personhendelse.inntekt

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class InntektOgVedtakEndringTest {
    @Test
    fun `Skal hvis bare feriepenger returner true for harIngenEksisterendeYtelser()`() {
        val inntektsendringer = opprettInntektOgVedtakEndring("feriepengerPleiepenger")

        Assertions.assertThat(inntektsendringer.harIngenEksisterendeYtelser()).isTrue()
    }

    @Test
    fun `Skal returnere false hvis har vanlig ytelse for harIngenEksisterendeYtelser()`() {
        val inntektsendringer = opprettInntektOgVedtakEndring("arbeidsavklaringspenger")

        Assertions.assertThat(inntektsendringer.harIngenEksisterendeYtelser()).isFalse()
    }

    @Test
    fun `Skal returnere true hvis null for harIngenEksisterendeYtelser()`() {
        val inntektsendringer = opprettInntektOgVedtakEndring(null)

        Assertions.assertThat(inntektsendringer.harIngenEksisterendeYtelser()).isTrue()
    }

    @Test
    fun `Skal returnere false hvis feriePenger og vanlig ytelse for harIngenEksisterendeYtelser()`() {
        val inntektsendringer = opprettInntektOgVedtakEndring("feriepengerPleiepenger, arbeidsavklaringspenger")

        Assertions.assertThat(inntektsendringer.harIngenEksisterendeYtelser()).isFalse()
    }

    private fun opprettInntektOgVedtakEndring(eksisterneYtelser: String?): InntektOgVedtakEndring {
        val beregningResultat = BeregningResultat(10000, 10, 123 / 3)

        return InntektOgVedtakEndring(
            personIdent = "",
            harNyeVedtak = true,
            prosessertTid = LocalDateTime.now(),
            inntektsendringFireMånederTilbake = beregningResultat,
            inntektsendringTreMånederTilbake = beregningResultat,
            inntektsendringToMånederTilbake = beregningResultat,
            inntektsendringForrigeMåned = beregningResultat,
            nyeYtelser = null,
            eksisterendeYtelser = eksisterneYtelser,
        )
    }
}
