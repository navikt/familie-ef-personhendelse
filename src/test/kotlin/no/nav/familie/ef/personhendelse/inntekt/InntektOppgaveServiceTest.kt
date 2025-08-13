package no.nav.familie.ef.personhendelse.inntekt

import io.mockk.mockk
import no.nav.familie.ef.personhendelse.client.ArbeidsfordelingClient
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.inntekt.oppgave.InntektOppgaveService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

class InntektOppgaveServiceTest {
    private val oppgaveClient = mockk<OppgaveClient>()
    private val sakClient = mockk<SakClient>()
    private val arbeidsfordelingClient = mockk<ArbeidsfordelingClient>()
    private val inntektsendringerRepository = mockk<InntektsendringerRepository>()
    private val taskService = mockk<TaskService>()

    val inntektOppgaveService = InntektOppgaveService(oppgaveClient, sakClient, arbeidsfordelingClient, inntektsendringerRepository, taskService, objectMapper)

    @Test
    fun `lagOppgavetekstForInntektsendring - sjekk tusenskille på feiltubetalingsbeløp og norsk format på år-måned`() {
        val oppgavetekst =
            inntektOppgaveService.lagOppgavetekstForInntektsendring(
                totalFeilutbetaling = 40006,
                yearMonthProssertTid = YearMonth.of(2023, 11),
            )

        Assertions.assertThat(oppgavetekst).contains("Beregnet feilutbetaling i uttrekksperioden: 40 006")
        Assertions.assertThat(oppgavetekst).contains("FOM 08.2023 - TOM 10.2023")
    }
}
