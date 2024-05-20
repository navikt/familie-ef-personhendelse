package no.nav.familie.ef.personhendelse.inntekt

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class FeilutbetalingBeregningUtilTest {
    @Test
    fun `beregn feilutbetaling - max feilutbetaling`() {
        val feilutbetaling = beregnFeilutbetalingForMåned(0, 60_000)
        val maxOvergangsstønadPrMnd = 118_620 * 2.25 / 12

        Assertions.assertEquals(maxOvergangsstønadPrMnd.toInt(), feilutbetaling)
    }

    @Test
    fun `beregn feilutbetaling`() {
        val feilutbetaling = beregnFeilutbetalingForMåned(18616, 33866)
        // Årsinntekt 223400 utbetaler 16088 pr mnd og 406400 utbetaler 9225
        // 223400 / 12 = 18616 , 406 400 / 12 = 33866
        Assertions.assertEquals(6863, feilutbetaling)
    }
}
