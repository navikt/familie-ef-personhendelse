package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.inntekt.endring.Grunnbeløp
import org.junit.Test

class GrunnbeløpTest {
    @Test
    fun `test at grunnbeløp er oppdatert`() {
        val grunnbeløp = Grunnbeløp().nyesteGrunnbeløp.grunnbeløp
        assert(grunnbeløp == 130_160.toBigDecimal()) { "Grunnbeløp er ikke oppdatert" }
    }

    @Test
    fun `test utregning for halvt grunnbeløp`() {
        val halvtGrunnbeløp = Grunnbeløp().nyesteGrunnbeløp.halvtGrunnbeløp
        assert(halvtGrunnbeløp == 65_080.toBigDecimal()) { "Halvt grunnbeløp er feil utregnet" }
    }

    @Test
    fun `test utregning for maks inntekt`() {
        val maksInntekt = Grunnbeløp().maksInntekt
        assert(maksInntekt == 715_000.toDouble()) { "Maks inntekt er feil" }
    }
}
