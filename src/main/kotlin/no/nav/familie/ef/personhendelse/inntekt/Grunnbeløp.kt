package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.kontrakter.felles.Månedsperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class GrunnbeløpVerdier(
    val periode: Månedsperiode,
    val grunnbeløp: BigDecimal,
    val halvtGrunnbeløp: BigDecimal = grunnbeløp.divide(BigDecimal(2), 0, java.math.RoundingMode.DOWN),
)

class Grunnbeløp {
    val reduksjonsfaktor = BigDecimal(0.45)

    val grunnbeløpsperioder: List<GrunnbeløpVerdier> =
        listOf(
            GrunnbeløpVerdier(
                periode = Månedsperiode(YearMonth.parse("2025-05"), YearMonth.from(LocalDate.MAX)),
                grunnbeløp = 130_160.toBigDecimal(),
            ),
            GrunnbeløpVerdier(
                periode = Månedsperiode("2024-05" to "2025-04"),
                grunnbeløp = 124_028.toBigDecimal(),
            ),
        )

    val nyesteGrunnbeløp: GrunnbeløpVerdier =
        grunnbeløpsperioder.first()

    val maksInntekt = Math.floor((nyesteGrunnbeløp.grunnbeløp.toInt() * 5.5) / 1000L) * 1000L // Ingen utbetaling av OS ved inntekt på over 5.5 rundet ned til nærmeste 1000
}
