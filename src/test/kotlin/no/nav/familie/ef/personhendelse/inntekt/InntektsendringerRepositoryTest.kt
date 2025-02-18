package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class InntektsendringerRepositoryTest : IntegrasjonSpringRunnerTest() {
    @Autowired
    lateinit var inntektsendringerRepository: InntektsendringerRepository

    @Test
    fun `lagre og hent ut inntektsendringer`() {
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
}
