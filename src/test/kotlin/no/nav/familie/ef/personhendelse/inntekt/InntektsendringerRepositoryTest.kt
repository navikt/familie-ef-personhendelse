package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.ef.personhendelse.inntekt.endring.BeregningResultat
import no.nav.familie.ef.personhendelse.inntekt.endring.Inntektsendring
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class InntektsendringerRepositoryTest : IntegrasjonSpringRunnerTest() {
    @Autowired
    lateinit var inntektsendringerRepository: InntektsendringerRepository

    @Test
    fun `lagre og hent ut inntektsendringer`() {
        val beregningResultatList = lagreInntektsendringForPersonIdent()

        val inntektsendringer = inntektsendringerRepository.hentInntektsendringerForUføretrygd()
        Assertions.assertThat(inntektsendringer.size).isEqualTo(1)
        Assertions.assertThat(inntektsendringer.first().personIdent).isEqualTo("01010199999")
        Assertions.assertThat(inntektsendringer.first().harNyeVedtak).isEqualTo(true)
        Assertions.assertThat(inntektsendringer.first().nyeYtelser).isEqualTo("ufoeretrygd")
        Assertions.assertThat(inntektsendringer.first().inntektsendringFireMånederTilbake).isEqualTo(beregningResultatList[0])
        Assertions.assertThat(inntektsendringer.first().inntektsendringTreMånederTilbake).isEqualTo(beregningResultatList[1])
        Assertions.assertThat(inntektsendringer.first().inntektsendringToMånederTilbake).isEqualTo(beregningResultatList[2])
        Assertions.assertThat(inntektsendringer.first().inntektsendringForrigeMåned).isEqualTo(beregningResultatList[3])
        Assertions.assertThat(inntektsendringer.first().eksisterendeYtelser).isEqualTo("sykepenger")
    }

    @Test
    fun `finn inntektsendringer som skal ha oppgave - endring må over 10 prosent og 30 000 i feilutbetaling`() {
        lagreInntektsendringGittFeilutbetalingsbeløp(31_000)

        val inntektsendringer = inntektsendringerRepository.hentInntektsendringerSomSkalHaOppgave()
        Assertions.assertThat(inntektsendringer.size).isEqualTo(1)
    }

    @Test
    fun `finn inntektsendringer som er kandidat til automatisk revurdering - endring må være over 10 prosent og mellom 5000 til 30000 i feilutbetaling`() {
        lagreInntektsendringGittFeilutbetalingsbeløp(15_000)

        val inntektsendringer = inntektsendringerRepository.hentKandidaterTilAutomatiskRevurdering()
        Assertions.assertThat(inntektsendringer.size).isEqualTo(1)
    }

    private fun lagreInntektsendringForPersonIdent(): List<BeregningResultat> {
        val beregningResultatList =
            listOf(
                BeregningResultat(1, 2, 3),
                BeregningResultat(4, 5, 6),
                BeregningResultat(7, 8, 9),
                BeregningResultat(10, 11, 12),
            )
        inntektsendringerRepository.lagreVedtakOgInntektsendringForPersonIdent(
            personIdent = "01010199999",
            harNyeVedtak = true,
            nyeYtelser = "ufoeretrygd",
            inntektsendring =
                Inntektsendring(
                    fireMånederTilbake = beregningResultatList[0],
                    treMånederTilbake = beregningResultatList[1],
                    toMånederTilbake = beregningResultatList[2],
                    forrigeMåned = beregningResultatList[3],
                ),
            eksisterendeYtelser = "sykepenger",
        )
        return beregningResultatList
    }

    private fun lagreInntektsendringGittFeilutbetalingsbeløp(feilutbetalingsbeløp: Int): List<BeregningResultat> {
        val beregningResultatList =
            listOf(
                BeregningResultat(10000, 10, feilutbetalingsbeløp / 3),
                BeregningResultat(10000, 10, feilutbetalingsbeløp / 3),
                BeregningResultat(10000, 10, feilutbetalingsbeløp / 3),
            )
        inntektsendringerRepository.lagreVedtakOgInntektsendringForPersonIdent(
            personIdent = "01010199998",
            harNyeVedtak = true,
            nyeYtelser = null,
            inntektsendring =
                Inntektsendring(
                    fireMånederTilbake = BeregningResultat(0, 0, 0),
                    treMånederTilbake = beregningResultatList[0],
                    toMånederTilbake = beregningResultatList[1],
                    forrigeMåned = beregningResultatList[2],
                ),
            eksisterendeYtelser = "sykepenger",
        )
        return beregningResultatList
    }
}
