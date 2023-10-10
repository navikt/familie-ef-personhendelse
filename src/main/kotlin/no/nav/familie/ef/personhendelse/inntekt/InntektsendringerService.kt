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

    private val halvtGrunnbeløpMånedlig = (118620 / 2) / 12

    fun beregnEndretInntekt(inntektshistorikkResponse: InntektshistorikkResponse, forventetInntektForPerson: ForventetInntektForPerson): Inntektsendring {
        // hent alle registrerte vedtak som var på personen sist beregning
        val nyesteRegistrerteInntekt =
            inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(1))
        val nestNyesteRegistrerteInntekt =
            inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(2))
        val inntektTreMånederTilbake =
            inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(3))

        val inntektsendringTreMånederTilbake = beregnInntektsendring(
            inntektTreMånederTilbake,
            forventetInntektForPerson.personIdent,
            forventetInntektForPerson.forventetInntektTreMånederTilbake,
        )
        val inntektsendringToMånederTilbake = beregnInntektsendring(
            nestNyesteRegistrerteInntekt,
            forventetInntektForPerson.personIdent,
            forventetInntektForPerson.forventetInntektToMånederTilbake,
        )
        val inntektsendringForrigeMåned = beregnInntektsendring(
            nyesteRegistrerteInntekt,
            forventetInntektForPerson.personIdent,
            forventetInntektForPerson.forventetInntektForrigeMåned,
        )

        return Inntektsendring(
            treMånederTilbake = inntektsendringTreMånederTilbake,
            toMånederTilbake = inntektsendringToMånederTilbake.prosent,
            beløpToMånederTilbake = inntektsendringToMånederTilbake.beløp,
            forrigeMåned = inntektsendringForrigeMåned.prosent,
            beløpForrigeMåned = inntektsendringForrigeMåned.beløp,
        )
    }

    private fun beregnInntektsendring(nyesteRegistrerteInntekt: List<InntektVersjon>?, ident: String, forventetInntekt: Int?): BeregningResultat {
        if (forventetInntekt == null || nyesteRegistrerteInntekt?.maxOfOrNull { it.versjon } == null) {
            secureLogger.warn("Ingen gjeldende inntekt funnet på person $ident har personen løpende stønad?")
            return BeregningResultat(0, 0)
        }
        if (forventetInntekt > 585000) return BeregningResultat(0, 0) // Ignorer alle med over 585000 i årsinntekt, da de har 0 i utbetaling.
        val månedligForventetInntekt = (forventetInntekt / 12)

        val orgNrToNyesteVersjonMap = nyesteRegistrerteInntekt.associate { it.opplysningspliktig to it.versjon }
        val inntektListe = nyesteRegistrerteInntekt.filter {
            it.versjon == orgNrToNyesteVersjonMap[it.opplysningspliktig] && it.arbeidsInntektInformasjon.inntektListe != null
        }.flatMap { it.arbeidsInntektInformasjon.inntektListe!! }
        val samletInntekt = inntektListe.filterNot {
            ignorerteYtelserOgUtbetalinger.contains(it.beskrivelse) ||
                (it.inntektType == InntektType.YTELSE_FRA_OFFENTLIGE && it.tilleggsinformasjon?.tilleggsinformasjonDetaljer?.detaljerType == "ETTERBETALINGSPERIODE")
        }.sumOf { it.beløp }

        if (samletInntekt < halvtGrunnbeløpMånedlig) return BeregningResultat(0, 0)

        secureLogger.info("Samlet inntekt: $samletInntekt - månedlig forventet inntekt: $månedligForventetInntekt  (årlig: $forventetInntekt) for person $ident")
        val inntektsendringProsent = (((samletInntekt - månedligForventetInntekt) / månedligForventetInntekt.toDouble()) * 100).toInt()
        val beløp = samletInntekt - månedligForventetInntekt
        if (månedligForventetInntekt == 0) return BeregningResultat(beløp, 100) // Prioriterer personer registrert med uredusert stønad, men har samlet inntekt over 1/2 G
        return BeregningResultat(beløp, inntektsendringProsent)
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

data class BeregningResultat(
    val beløp: Int,
    val prosent: Int,
)
