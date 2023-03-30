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

    private val halvtGrunnbeløpMånedlig = (111477 / 2) / 12

    fun beregnEndretInntekt(inntektshistorikkResponse: InntektshistorikkResponse, forventetInntektForPerson: ForventetInntektForPerson): Inntektsendring {
        // hent alle registrerte vedtak som var på personen sist beregning
        val nyesteRegistrerteInntekt =
            inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(1))
        val nestNyesteRegistrerteInntekt =
            inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(2))

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

        return Inntektsendring(toMånederTilbake = inntektsendringToMånederTilbake, forrigeMåned = inntektsendringForrigeMåned)
    }

    private fun beregnInntektsendring(nyesteRegistrerteInntekt: List<InntektVersjon>?, ident: String, forventetInntekt: Int?): Int {
        if (forventetInntekt == null || nyesteRegistrerteInntekt?.maxOfOrNull { it.versjon } == null) {
            secureLogger.warn("Ingen gjeldende inntekt funnet på person $ident har personen løpende stønad?")
            return 0
        }
        if (forventetInntekt > 585000) return 0 // Ignorer alle med over 585000 i årsinntekt, da de har 0 i utbetaling.
        val månedligForventetInntekt = (forventetInntekt / 12)

        val orgNrToNyesteVersjonMap = nyesteRegistrerteInntekt.associate { it.opplysningspliktig to it.versjon }
        val inntektListe = nyesteRegistrerteInntekt.filter {
            it.versjon == orgNrToNyesteVersjonMap[it.opplysningspliktig] && it.arbeidsInntektInformasjon.inntektListe != null
        }.flatMap { it.arbeidsInntektInformasjon.inntektListe!! }
        val samletInntekt = inntektListe.filterNot {
            ignorerteYtelserOgUtbetalinger.contains(it.beskrivelse) ||
                (it.inntektType == InntektType.YTELSE_FRA_OFFENTLIGE && it.tilleggsinformasjon?.tilleggsinformasjonDetaljer?.detaljerType == "ETTERBETALINGSPERIODE")
        }.sumOf { it.beløp }

        if (samletInntekt < halvtGrunnbeløpMånedlig) return 0
        if (månedligForventetInntekt == 0) return 100 // Prioriterer personer registrert med uredusert stønad, men har samlet inntekt over 1/2 G

        secureLogger.info("Samlet inntekt: $samletInntekt - månedlig forventet inntekt: $månedligForventetInntekt  (årlig: $forventetInntekt) for person $ident")
        val inntektsendring = (((samletInntekt - månedligForventetInntekt) / månedligForventetInntekt.toDouble()) * 100).toInt()
        return inntektsendring
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
