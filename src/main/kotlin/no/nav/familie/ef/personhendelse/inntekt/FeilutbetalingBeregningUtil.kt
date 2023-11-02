package no.nav.familie.ef.personhendelse.inntekt

import java.math.BigDecimal
import java.math.RoundingMode

private val grunnbeløp = 118_620
private val reduksjonsfaktor = BigDecimal(0.45)

fun beregnFeilutbetaling(forventetInntekt: Int, samletInntekt: Int): Int {
    return beregnUtbetaling(forventetInntekt) - beregnUtbetaling(samletInntekt)
}

private fun beregnUtbetaling(inntekt: Int): Int {
    val avkortningPerMåned = beregnAvkortning(inntekt).divide(BigDecimal(12)).setScale(0, RoundingMode.HALF_DOWN)

    val fullOvergangsstønadPerMåned =
        BigDecimal(grunnbeløp).multiply(BigDecimal(2.25)).divide(BigDecimal(12)).setScale(0, RoundingMode.HALF_EVEN)

    val utbetaling = fullOvergangsstønadPerMåned.subtract(avkortningPerMåned).setScale(0, RoundingMode.HALF_UP)

    return if (utbetaling <= BigDecimal.ZERO) 0 else utbetaling.intValueExact()
}

private fun beregnAvkortning(inntekt: Int): BigDecimal {
    val inntektOverHalveGrunnbeløp = BigDecimal(inntekt).subtract(BigDecimal(grunnbeløp).multiply(BigDecimal(0.5)))
    return if (inntektOverHalveGrunnbeløp > BigDecimal.ZERO) {
        inntektOverHalveGrunnbeløp.multiply(reduksjonsfaktor).setScale(5, RoundingMode.HALF_DOWN)
    } else {
        BigDecimal.ZERO
    }
}
