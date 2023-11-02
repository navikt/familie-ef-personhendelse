package no.nav.familie.ef.personhendelse.inntekt

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class FeilutbetalingBeregningUtilTest {

    @Test
    fun `beregn feilutbetaling - max`() {
        val feilutbetaling = beregnFeilutbetaling(0, 700_000)
        val maxOvergangsstønadPrMnd = 118_620 * 2.25 / 12

        Assertions.assertEquals(maxOvergangsstønadPrMnd.toInt(), feilutbetaling)
    }

    @Test
    fun `beregn feilutbetaling`() {
        val feilutbetaling = beregnFeilutbetaling(223_400, 406_400)
        // 223400 utbetaler 16088 og 406400 utbetaler 9225
        Assertions.assertEquals(6863, feilutbetaling)
    }
}
