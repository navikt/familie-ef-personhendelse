package no.nav.familie.ef.personhendelse.inntekt

import java.time.YearMonth

object VedtakendringerUtil {
    fun harNyeVedtak(inntektResponse: InntektResponse): Boolean = nyeVedtak(inntektResponse).isNotEmpty()

    fun offentligeYtelserForNyesteMåned(inntektResponse: InntektResponse): List<String> {
        val nyesteRegistrerteInntekt = inntektResponse.månedsData.filter { it.måned == YearMonth.now().minusMonths(1) }
        return offentligeYtelser(nyesteRegistrerteInntekt)
    }

    fun nyeVedtak(inntektResponse: InntektResponse): List<String> {
        val nyesteRegistrerteInntekt = inntektResponse.månedsData.filter { it.måned == YearMonth.now().minusMonths(1) }
        val nestNyesteRegistrerteInntekt = inntektResponse.månedsData.filter { it.måned == YearMonth.now().minusMonths(2) }

        val offentligeYtelserForNyesteMåned = offentligeYtelser(nyesteRegistrerteInntekt)
        val offentligeYtelserForNestNyesteMåned = offentligeYtelser(nestNyesteRegistrerteInntekt)

        return offentligeYtelserForNyesteMåned.minus(offentligeYtelserForNestNyesteMåned.toSet())
    }

    private fun offentligeYtelser(inntektMåneder: List<InntektMåned>): List<String> =
        inntektMåneder
            .flatMap { it.inntektListe }
            .filter {
                it.type == InntektType.YTELSE_FRA_OFFENTLIGE &&
                    it.beskrivelse != "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere" &&
                    it.tilleggsinformasjon?.type != "ETTERBETALINGSPERIODE"
            }.map { it.beskrivelse }
            .distinct()
}
