package no.nav.familie.ef.personhendelse.inntekt.vedtak

import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.ef.personhendelse.inntekt.BeregningResultat
import no.nav.familie.ef.personhendelse.inntekt.Inntektsendring
import no.nav.familie.kontrakter.felles.ef.EnsligForsørgerVedtakhendelse
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.YearMonth

class EfVedtakRepositoryTest : IntegrasjonSpringRunnerTest() {

    @Autowired
    lateinit var efVedtakRepository: EfVedtakRepository

    @Test
    fun `lagre og hent EnsligForsørgerVedtakshendelse`() {
        val efVedtakshendelse = EnsligForsørgerVedtakhendelse(1L, "personIdent1", StønadType.OVERGANGSSTØNAD)
        efVedtakRepository.lagreEfVedtakshendelse(efVedtakshendelse)

        val vedtakshendelse = efVedtakRepository.hentAllePersonerMedVedtak().first()
        Assertions.assertThat(vedtakshendelse).isNotNull
        Assertions.assertThat(vedtakshendelse.behandlingId).isEqualTo(1L)
        Assertions.assertThat(StønadType.OVERGANGSSTØNAD).isEqualTo(vedtakshendelse.stønadType)
        Assertions.assertThat(YearMonth.now()).isEqualTo(vedtakshendelse.aarMaanedProsessert)
        Assertions.assertThat(1).isEqualTo(vedtakshendelse.versjon)
    }

    @Test
    fun `lagre og hent EnsligForsørgerVedtakshendelser med to like personidenter, men to ulike behandlingId`() {
        val efVedtakshendelse = EnsligForsørgerVedtakhendelse(1L, "personIdent1", StønadType.OVERGANGSSTØNAD)
        efVedtakRepository.lagreEfVedtakshendelse(efVedtakshendelse)

        val efVedtakshendelse2 = EnsligForsørgerVedtakhendelse(2L, "personIdent1", StønadType.OVERGANGSSTØNAD)
        efVedtakRepository.lagreEfVedtakshendelse(efVedtakshendelse2)

        val vedtakshendelse = efVedtakRepository.hentAllePersonerMedVedtak().first()
        Assertions.assertThat(vedtakshendelse).isNotNull
        Assertions.assertThat(vedtakshendelse.behandlingId).isEqualTo(2L)
        Assertions.assertThat(vedtakshendelse.stønadType).isEqualTo(StønadType.OVERGANGSSTØNAD)
        Assertions.assertThat(vedtakshendelse.aarMaanedProsessert).isEqualTo(YearMonth.now())
        Assertions.assertThat(vedtakshendelse.versjon).isEqualTo(1)
    }

    @Test
    fun `lagre og hent ikke behandlede ensligForsørgerVedtakshendelse`() {
        val efVedtakshendelse = EnsligForsørgerVedtakhendelse(2L, "personIdent2", StønadType.OVERGANGSSTØNAD)
        efVedtakRepository.lagreEfVedtakshendelse(efVedtakshendelse)

        val vedtakshendelse = efVedtakRepository.hentPersonerMedVedtakIkkeBehandlet()
        Assertions.assertThat(vedtakshendelse).isNotNull
        Assertions.assertThat(vedtakshendelse.size).isEqualTo(0)
    }

    @Test
    fun `lagre og oppdater ensligForsørgerVedtakshendelse`() {
        val efVedtakshendelse = EnsligForsørgerVedtakhendelse(3L, "personIdent3", StønadType.OVERGANGSSTØNAD)
        efVedtakRepository.lagreEfVedtakshendelse(efVedtakshendelse)
        efVedtakRepository.oppdaterAarMaanedProsessert("personIdent3", YearMonth.of(2021, 12))

        val vedtakshendelseList = efVedtakRepository.hentPersonerMedVedtakIkkeBehandlet()
        Assertions.assertThat(vedtakshendelseList).isNotNull
        Assertions.assertThat(vedtakshendelseList.size).isEqualTo(1)
        Assertions.assertThat(vedtakshendelseList.first().aarMaanedProsessert).isEqualTo(YearMonth.of(2021, 12))
    }

    @Test
    fun `lagre inntektsendringer`() {
        efVedtakRepository.lagreVedtakOgInntektsendringForPersonIdent(
            "1",
            true,
            "SYKEPENGER, UFØRETRYGD",
            Inntektsendring(
                BeregningResultat(150, 15, 100),
                BeregningResultat(250, 10, 50),
                BeregningResultat(350, 5, 25),
                BeregningResultat(500, 1, 12),
            ),
        )
        var hentInntektsendringer = efVedtakRepository.hentInntektOgVedtakEndring()
        Assertions.assertThat(hentInntektsendringer.size).isEqualTo(1)

        val inntektsendring = hentInntektsendringer.first()
        Assertions.assertThat(inntektsendring.personIdent).isEqualTo("1")
        Assertions.assertThat(inntektsendring.harNyeVedtak).isTrue
        Assertions.assertThat(inntektsendring.nyeYtelser).isEqualTo("SYKEPENGER, UFØRETRYGD")
        Assertions.assertThat(inntektsendring.inntektsendringFireMånederTilbake.beløp).isEqualTo(150)
        Assertions.assertThat(inntektsendring.inntektsendringTreMånederTilbake.beløp).isEqualTo(250)
        Assertions.assertThat(inntektsendring.inntektsendringToMånederTilbake.beløp).isEqualTo(350)
        Assertions.assertThat(inntektsendring.inntektsendringForrigeMåned.beløp).isEqualTo(500)
        Assertions.assertThat(inntektsendring.inntektsendringFireMånederTilbake.prosent).isEqualTo(15)
        Assertions.assertThat(inntektsendring.inntektsendringTreMånederTilbake.prosent).isEqualTo(10)
        Assertions.assertThat(inntektsendring.inntektsendringToMånederTilbake.prosent).isEqualTo(5)
        Assertions.assertThat(inntektsendring.inntektsendringForrigeMåned.prosent).isEqualTo(1)
        Assertions.assertThat(inntektsendring.inntektsendringFireMånederTilbake.feilutbetaling).isEqualTo(100)
        Assertions.assertThat(inntektsendring.inntektsendringTreMånederTilbake.feilutbetaling).isEqualTo(50)
        Assertions.assertThat(inntektsendring.inntektsendringToMånederTilbake.feilutbetaling).isEqualTo(25)
        Assertions.assertThat(inntektsendring.inntektsendringForrigeMåned.feilutbetaling).isEqualTo(12)

        efVedtakRepository.clearInntektsendringer()
        hentInntektsendringer = efVedtakRepository.hentInntektOgVedtakEndring()
        Assertions.assertThat(hentInntektsendringer.isEmpty()).isTrue
    }
}
