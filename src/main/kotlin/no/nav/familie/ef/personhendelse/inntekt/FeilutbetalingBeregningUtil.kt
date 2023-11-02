package no.nav.familie.ef.personhendelse.inntekt

import java.math.BigDecimal
import java.math.RoundingMode

private val grunnbeløp = 118_620
private val halvtGrunnbeløp = 59_310
private val reduksjonsfaktor = BigDecimal(0.45)

fun beregnFeilutbetalingForMåned(forventetInntekt: Int, samletInntekt: Int): Int {
    return beregnUtbetaling(forventetInntekt) - beregnUtbetaling(samletInntekt)
}

private fun beregnUtbetaling(inntekt: Int): Int {
    val avkortning = beregnAvkortning(inntekt).setScale(0, RoundingMode.HALF_DOWN)

    val fullOvergangsstønad =
        BigDecimal(grunnbeløp).multiply(BigDecimal(2.25)).setScale(0, RoundingMode.HALF_EVEN)

    val utbetaling = fullOvergangsstønad.subtract(avkortning).setScale(0, RoundingMode.HALF_UP).toInt()

    return if (utbetaling <= 0) 0 else utbetaling / 12
}

private fun beregnAvkortning(inntekt: Int): BigDecimal {
    val inntektOverHalveGrunnbeløpÅrlig = BigDecimal(inntekt).multiply(BigDecimal(12)).subtract(BigDecimal(halvtGrunnbeløp))
    return if (inntektOverHalveGrunnbeløpÅrlig > BigDecimal.ZERO) {
        inntektOverHalveGrunnbeløpÅrlig.multiply(reduksjonsfaktor).setScale(5, RoundingMode.HALF_DOWN)
    } else {
        BigDecimal.ZERO
    }
}
