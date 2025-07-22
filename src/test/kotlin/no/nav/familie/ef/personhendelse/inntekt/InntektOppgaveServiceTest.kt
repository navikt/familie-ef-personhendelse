package no.nav.familie.ef.personhendelse.inntekt

import io.mockk.mockk
import no.nav.familie.ef.personhendelse.client.ArbeidsfordelingClient
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.pdl.PdlClient
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class InntektOppgaveServiceTest {
    private val oppgaveClient = mockk<OppgaveClient>()
    private val sakClient = mockk<SakClient>()
    private val arbeidsfordelingClient = mockk<ArbeidsfordelingClient>()
    private val inntektsendringerRepository = mockk<InntektsendringerRepository>()
    private val inntektsendringerService = mockk<InntektsendringerService>()
    private val pdlClient = mockk<PdlClient>()

    val inntektOppgaveService = InntektOppgaveService(oppgaveClient, sakClient, arbeidsfordelingClient, inntektsendringerRepository, inntektsendringerService, pdlClient)

    @Test
    fun `lagOppgavetekstForInntektsendring - sjekk tusenskille på feiltubetalingsbeløp og norsk format på år-måned`() {
        val oppgavetekst =
            inntektOppgaveService.lagOppgavetekstForInntektsendring(
                InntektOgVedtakEndring(
                    personIdent = "1",
                    harNyeVedtak = false,
                    prosessertTid = LocalDateTime.of(2023, 11, 8, 5, 0),
                    inntektsendringFireMånederTilbake = BeregningResultat(1, 1, 1),
                    inntektsendringTreMånederTilbake = BeregningResultat(2, 2, 2),
                    inntektsendringToMånederTilbake = BeregningResultat(3, 3, 3),
                    inntektsendringForrigeMåned = BeregningResultat(4, 4, 40000),
                    nyeYtelser = null,
                    eksisterendeYtelser = null,
                ),
            )

        Assertions.assertThat(oppgavetekst.contains("Beregnet feilutbetaling i uttrekksperioden: 40 006 kroner "))
        Assertions.assertThat(oppgavetekst.contains("FOM 06.2023 - TOM 10.2023"))
    }
}
