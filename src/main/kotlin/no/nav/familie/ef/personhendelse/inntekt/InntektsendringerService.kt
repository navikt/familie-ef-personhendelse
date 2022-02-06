package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.pdl.secureLogger
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class InntektsendringerService(
    val oppgaveClient: OppgaveClient,
    val sakClient: SakClient
) {

    fun harEndretInntekt(inntektshistorikkResponse: InntektshistorikkResponse, identMedForventetInntekt: Map.Entry<String, Int?>): Boolean {
        // hent alle registrerte vedtak som var på personen sist beregning
        val nyesteRegistrerteInntekt = inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(1).toString())
        val nestNyesteRegistrerteInntekt = inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(2).toString())

        return har10ProsentHøyereInntektEnnForventet(nyesteRegistrerteInntekt, identMedForventetInntekt)
    }

    private fun har10ProsentHøyereInntektEnnForventet(nyesteRegistrerteInntekt: List<InntektVersjon>?, identMedForventetInntekt: Map.Entry<String, Int?>): Boolean {

        if (identMedForventetInntekt.value == null) {
            secureLogger.warn("Ingen gjeldende inntekt funnet på person ${identMedForventetInntekt.key} har personen løpende stønad?")
            return false
        }
        val månedligForventetInntekt = (identMedForventetInntekt.value!! / 12)

        val nyesteVersjon = nyesteRegistrerteInntekt?.maxOf { it.versjon }
        val inntektListe = nyesteRegistrerteInntekt?.firstOrNull { it.versjon == nyesteVersjon }?.arbeidsInntektInformasjon?.inntektListe

        val samletInntekt = inntektListe?.filter { it.inntektType != InntektType.YTELSE_FRA_OFFENTLIGE }?.sumOf { it.beløp } ?: 0
        secureLogger.info("Samlet inntekt: $samletInntekt - månedlig forventet inntekt: $månedligForventetInntekt  (årlig: ${identMedForventetInntekt.value}) for person ${identMedForventetInntekt.key}")
        return samletInntekt >= (månedligForventetInntekt * 1.1) && samletInntekt > 0 // Må sjekke om samletInntekt er større enn 0 for å ikke få true dersom alle variabler er 0 (antakelig kun i test)
    }

    // Denne kan være vanskelig å bruke, da det kan være mye etterbetalinger som gir falsk positiv
    private fun harMottattMerIOffentligeYtelser(nestNyesteRegistrerteInntekt: List<InntektVersjon>?, nyesteRegistrerteInntekt: List<InntektVersjon>?): Boolean {
        val sumOffentligeYtelserForNyeste = sumOffentligeYtelser(nyesteRegistrerteInntekt)
        val sumOffentligeYtelserForNestNyeste = sumOffentligeYtelser(nestNyesteRegistrerteInntekt)

        return (sumOffentligeYtelserForNyeste > sumOffentligeYtelserForNestNyeste)
    }

    private fun sumOffentligeYtelser(nyesteRegistrerteInntekt: List<InntektVersjon>?): Int {
        val nyesteVersjon = nyesteRegistrerteInntekt?.maxOf { it.versjon }

        val inntektListe = nyesteRegistrerteInntekt?.firstOrNull { it.versjon == nyesteVersjon }?.arbeidsInntektInformasjon?.inntektListe
        val beløpYtelseFraOffentligList = inntektListe?.filter {
            it.inntektType == InntektType.YTELSE_FRA_OFFENTLIGE && !ignorerteYtelserIBeregningAvInntekt.contains(it.beskrivelse)
        }?.map { it.beløp } ?: listOf() // Sjekker kun mot faste månedlige utbetalinger (ikke dagpenger / AAP fordi de ofte er variable)
        return beløpYtelseFraOffentligList.sumOf { it }
    }

    // Ignorterte ytelser: AAP og Dagpenger er ignorert fordi de er variable. Alle uføre går under annet regelverk (samordning) og skal derfor ignoreres.
    val ignorerteYtelserIBeregningAvInntekt = listOf("overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere", "arbeidsavklaringspenger", "dagpengerVedArbeidsloeshet", "ufoeretrygd", "ufoereytelseEtteroppgjoer")
}
