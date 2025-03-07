package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.inntekt.inntektv2.InntektTypeV2
import no.nav.familie.ef.personhendelse.inntekt.inntektv2.InntektV2Response
import no.nav.familie.ef.personhendelse.inntekt.inntektv2.MånedsInntekt
import java.time.YearMonth

object VedtakendringerUtil {
    fun harNyeVedtak(inntektResponse: InntektV2Response) = nyeVedtak(inntektResponse).isNotEmpty()

    fun offentligeYtelserForNyesteMåned(inntektResponse: InntektV2Response): List<String> {
        val nyesteRegistrerteInntekt = inntektResponse.maanedsData.filter { it.måned == YearMonth.now().minusMonths(1) }
        return offentligeYtelser(nyesteRegistrerteInntekt)
    }

    fun nyeVedtak(inntektResponse: InntektV2Response): List<String> {
        val nyesteRegistrerteInntekt = inntektResponse.maanedsData.filter { it.måned == YearMonth.now().minusMonths(1) }
        val nestNyesteRegistrerteInntekt = inntektResponse.maanedsData.filter { it.måned == YearMonth.now().minusMonths(2) }

        val offentligeYtelserForNyesteMåned = offentligeYtelser(nyesteRegistrerteInntekt)
        val offentligeYtelserForNestNyesteMåned = offentligeYtelser(nestNyesteRegistrerteInntekt)

        return offentligeYtelserForNyesteMåned.minus(offentligeYtelserForNestNyesteMåned)
    }

    private fun offentligeYtelser(nyesteRegistrerteInntekt: List<MånedsInntekt>): List<String> {
        val offentligYtelseInntekt1 =
            nyesteRegistrerteInntekt.filter {
                it.inntektListe.any { offentligYtelse ->
                    offentligYtelse.type == InntektTypeV2.YTELSE_FRA_OFFENTLIGE && offentligYtelse.beskrivelse != "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere"
                }
            }

        val inntektListe = offentligYtelseInntekt1.firstOrNull()?.inntektListe ?: emptyList()

        return inntektListe.filter { it.type == InntektTypeV2.YTELSE_FRA_OFFENTLIGE && it.beskrivelse != "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere" && it.tilleggsinformasjon?.type == "ETTERBETALINGSPERIODE" }.groupBy { it.beskrivelse }.map { it.key }
    }
}
