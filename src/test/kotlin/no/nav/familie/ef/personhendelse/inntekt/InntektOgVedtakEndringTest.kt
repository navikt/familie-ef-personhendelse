package no.nav.familie.ef.personhendelse.inntekt

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class InntektOgVedtakEndringTest {
    @Test
    fun `Skal hvis bare feriepenger returner true for harIngenEksisterendeYtelser()`() {
        val beregningResultatList =
            listOf(
                BeregningResultat(10000, 10, 123 / 3),
                BeregningResultat(10000, 10, 213 / 3),
                BeregningResultat(10000, 10, 213 / 3),
            )

        val inntektsendringer =
            InntektOgVedtakEndring(
                personIdent = "",
                harNyeVedtak = true,
                prosessertTid = LocalDateTime.now(),
                inntektsendringFireMånederTilbake = beregningResultatList[0],
                inntektsendringTreMånederTilbake = beregningResultatList[0],
                inntektsendringToMånederTilbake = beregningResultatList[0],
                inntektsendringForrigeMåned = beregningResultatList[0],
                nyeYtelser = null,
                eksisterendeYtelser = "feriepengerPleiepenger",
            )

        Assertions.assertThat(inntektsendringer.harIngenEksisterendeYtelser())
    }
}
