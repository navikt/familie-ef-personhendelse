package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.inntekt.inntektv2.InntektMåned
import no.nav.familie.ef.personhendelse.inntekt.inntektv2.InntektResponse
import no.nav.familie.ef.personhendelse.inntekt.inntektv2.InntektTypeV2
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

    private fun offentligeYtelser(inntektMåneder: List<InntektMåned>): List<String> {
        val offentligYtelseInntekt =
            inntektMåneder.filter {
                it.inntektListe.any { inntekt ->
                    inntekt.type == InntektTypeV2.YTELSE_FRA_OFFENTLIGE &&
                        inntekt.beskrivelse != "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere"
                }
            }

        val inntektListe = offentligYtelseInntekt.flatMap { it.inntektListe }

        return inntektListe
            .filter {
                it.type == InntektTypeV2.YTELSE_FRA_OFFENTLIGE &&
                    it.beskrivelse != "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere" &&
                    it.tilleggsinformasjon?.type != "ETTERBETALINGSPERIODE"
            }.groupBy { it.beskrivelse }
            .keys
            .toList()
    }
}
