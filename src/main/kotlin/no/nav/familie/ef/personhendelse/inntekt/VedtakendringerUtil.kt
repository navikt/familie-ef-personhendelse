package no.nav.familie.ef.personhendelse.inntekt

import java.time.YearMonth

object VedtakendringerUtil {
    fun harNyeVedtak(response: HentInntektListeResponse) = nyeVedtak(response)?.isNotEmpty() ?: false

    fun nyeVedtak(inntektResponse: HentInntektListeResponse): List<String>? {
        // hent alle registrerte vedtak som var på personen sist beregning
        val nyesteRegistrerteInntekt =
            inntektResponse.arbeidsinntektMåned?.filter { it.årMåned == YearMonth.now().minusMonths(1) }
        val nestNyesteRegistrerteInntekt =
            inntektResponse.arbeidsinntektMåned?.filter { it.årMåned == YearMonth.now().minusMonths(2) }

        val offentligeYtelserForNyesteMåned = offentligeYtelser(nyesteRegistrerteInntekt) ?: emptyList()
        val offentligeYtelserForNestNyesteMåned = offentligeYtelser(nestNyesteRegistrerteInntekt) ?: emptyList()

        return offentligeYtelserForNyesteMåned.minus(offentligeYtelserForNestNyesteMåned)
    }

    private fun offentligeYtelser(nyesteRegistrerteInntekt: List<ArbeidsinntektMåned>?): List<String>? {
        val offentligYtelseInntekt =
            nyesteRegistrerteInntekt?.filter {
                it.arbeidsInntektInformasjon?.inntektListe?.any { offentligYtelse ->
                    offentligYtelse.inntektType == InntektType.YTELSE_FRA_OFFENTLIGE &&
                        offentligYtelse.beskrivelse != "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere"
                }
                    ?: false
            }

        val inntektListe = offentligYtelseInntekt?.firstOrNull()?.arbeidsInntektInformasjon?.inntektListe
        return inntektListe
            ?.filter {
                it.inntektType == InntektType.YTELSE_FRA_OFFENTLIGE &&
                    it.beskrivelse != "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere" &&
                    it.tilleggsinformasjon?.tilleggsinformasjonDetaljer?.detaljerType != "ETTERBETALINGSPERIODE"
            }?.groupBy { it.beskrivelse }
            ?.mapNotNull { it.key }
    }
}
