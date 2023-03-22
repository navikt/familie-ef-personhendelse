package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.client.ForventetInntektForPerson
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.pdl.secureLogger
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class InntektsendringerService(
    val oppgaveClient: OppgaveClient,
    val sakClient: SakClient,
) {

    private val halvtGrunnbeløpMånedlig = (106399 / 2) / 12

    fun harEndretInntekt(inntektshistorikkResponse: InntektshistorikkResponse, forventetInntektForPerson: ForventetInntektForPerson): Boolean {
        // hent alle registrerte vedtak som var på personen sist beregning
        val nyesteRegistrerteInntekt = inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(1).toString())
        val nestNyesteRegistrerteInntekt = inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(2).toString())

        return har10ProsentHøyereInntektEnnForventet(nestNyesteRegistrerteInntekt, forventetInntektForPerson.personIdent, forventetInntektForPerson.forventetInntektToMånederTilbake) &&
            har10ProsentHøyereInntektEnnForventet(nyesteRegistrerteInntekt, forventetInntektForPerson.personIdent, forventetInntektForPerson.forventetInntektForrigeMåned)
    }

    private fun har10ProsentHøyereInntektEnnForventet(nyesteRegistrerteInntekt: List<InntektVersjon>?, ident: String, forventetInntekt: Int?): Boolean {
        if (forventetInntekt == null || nyesteRegistrerteInntekt?.maxOfOrNull { it.versjon } == null) {
            secureLogger.warn("Ingen gjeldende inntekt funnet på person $ident har personen løpende stønad?")
            return false
        }
        if (forventetInntekt > 585000) return false // Ignorer alle med over 585000 i årsinntekt, da de har 0 i utbetaling.
        val månedligForventetInntekt = (forventetInntekt / 12)

        val orgNrToNyesteVersjonMap = nyesteRegistrerteInntekt.associate { it.opplysningspliktig to it.versjon }
        val inntektListe = nyesteRegistrerteInntekt.filter { it.versjon == orgNrToNyesteVersjonMap.get(it.opplysningspliktig) && it.arbeidsInntektInformasjon.inntektListe != null }.flatMap { it.arbeidsInntektInformasjon.inntektListe!! }

        val samletInntekt = inntektListe.filterNot {
            ignorerteYtelserOgUtbetalinger.contains(it.beskrivelse) ||
                (it.inntektType == InntektType.YTELSE_FRA_OFFENTLIGE && it.tilleggsinformasjon?.tilleggsinformasjonDetaljer?.detaljerType == "ETTERBETALINGSPERIODE")
        }.sumOf { it.beløp }
        if (samletInntekt < halvtGrunnbeløpMånedlig) return false
        secureLogger.info("Samlet inntekt: $samletInntekt - månedlig forventet inntekt: $månedligForventetInntekt  (årlig: $forventetInntekt) for person $ident")
        return samletInntekt >= (månedligForventetInntekt * 1.1) && samletInntekt > 0 // Må sjekke om samletInntekt er større enn 0 for å ikke få true dersom alle variabler er 0 (antakelig kun i test)
    }

    // Ignorterte ytelser: AAP og Dagpenger er ignorert fordi de er variable. Alle uføre går under annet regelverk (samordning) og skal derfor ignoreres.
    val ignorerteYtelserOgUtbetalinger = listOf(
        "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere",
        "arbeidsavklaringspenger",
        "dagpengerVedArbeidsloeshet",
        "ufoeretrygd",
        "ufoereytelseEtteroppgjoer",
        "feriepenger",
        "ufoerepensjonFraAndreEnnFolketrygden",
    )
}
