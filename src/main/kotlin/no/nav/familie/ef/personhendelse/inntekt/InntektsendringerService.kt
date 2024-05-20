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
    private val grunnbeløp = 118_620
    private val halvtGrunnbeløpMånedlig = (grunnbeløp / 2) / 12

    fun beregnEndretInntekt(
        inntektshistorikkResponse: InntektshistorikkResponse,
        forventetInntektForPerson: ForventetInntektForPerson,
    ): Inntektsendring {
        // hent alle registrerte vedtak som var på personen sist beregning
        val nyesteRegistrerteInntekt =
            inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(1))
        val nestNyesteRegistrerteInntekt =
            inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(2))
        val inntektTreMånederTilbake =
            inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(3))
        val inntektFireMånederTilbake =
            inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(4))

        val inntektsendringFireMånederTilbake =
            beregnInntektsendring(
                inntektFireMånederTilbake,
                forventetInntektForPerson.personIdent,
                forventetInntektForPerson.forventetInntektTreMånederTilbake,
            )

        val inntektsendringTreMånederTilbake =
            beregnInntektsendring(
                inntektTreMånederTilbake,
                forventetInntektForPerson.personIdent,
                forventetInntektForPerson.forventetInntektTreMånederTilbake,
            )
        val inntektsendringToMånederTilbake =
            beregnInntektsendring(
                nestNyesteRegistrerteInntekt,
                forventetInntektForPerson.personIdent,
                forventetInntektForPerson.forventetInntektToMånederTilbake,
            )
        val inntektsendringForrigeMåned =
            beregnInntektsendring(
                nyesteRegistrerteInntekt,
                forventetInntektForPerson.personIdent,
                forventetInntektForPerson.forventetInntektForrigeMåned,
            )

        return Inntektsendring(
            fireMånederTilbake = inntektsendringFireMånederTilbake,
            treMånederTilbake = inntektsendringTreMånederTilbake,
            toMånederTilbake = inntektsendringToMånederTilbake,
            forrigeMåned = inntektsendringForrigeMåned,
        )
    }

    private fun beregnInntektsendring(
        nyesteRegistrerteInntekt: List<InntektVersjon>?,
        ident: String,
        forventetInntekt: Int?,
    ): BeregningResultat {
        if (forventetInntekt == null || nyesteRegistrerteInntekt?.maxOfOrNull { it.versjon } == null) {
            secureLogger.warn("Ingen gjeldende inntekt funnet på person $ident har personen løpende stønad?")
            return BeregningResultat(0, 0, 0)
        }

        if (forventetInntekt > 652000) return BeregningResultat(0, 0, 0) // Ignorer alle med over 652000 i årsinntekt, da de har 0 i utbetaling.
        val månedligForventetInntekt = (forventetInntekt / 12)

        val orgNrToNyesteVersjonMap = nyesteRegistrerteInntekt.associate { it.opplysningspliktig to it.versjon }
        val inntektListe =
            nyesteRegistrerteInntekt.filter {
                it.versjon == orgNrToNyesteVersjonMap[it.opplysningspliktig] && it.arbeidsInntektInformasjon.inntektListe != null
            }.flatMap { it.arbeidsInntektInformasjon.inntektListe!! }
        val samletInntekt =
            inntektListe.filterNot {
                ignorerteYtelserOgUtbetalinger.contains(it.beskrivelse) ||
                    (it.inntektType == InntektType.YTELSE_FRA_OFFENTLIGE && it.tilleggsinformasjon?.tilleggsinformasjonDetaljer?.detaljerType == "ETTERBETALINGSPERIODE")
            }.sumOf { it.beløp }

        if (samletInntekt < halvtGrunnbeløpMånedlig) return BeregningResultat(0, 0, 0)

        secureLogger.info(
            "Samlet inntekt: $samletInntekt - månedlig forventet inntekt: $månedligForventetInntekt  (årlig: $forventetInntekt) for person $ident",
        )
        val inntektsendringProsent = (((samletInntekt - månedligForventetInntekt) / månedligForventetInntekt.toDouble()) * 100).toInt()
        val endretInntektBeløp = samletInntekt - månedligForventetInntekt
        val feilutbetaling = beregnFeilutbetalingForMåned(månedligForventetInntekt, samletInntekt)
        if (månedligForventetInntekt == 0) return BeregningResultat(endretInntektBeløp, 100, feilutbetaling) // Prioriterer personer registrert med uredusert stønad, men har samlet inntekt over 1/2 G
        return BeregningResultat(endretInntektBeløp, inntektsendringProsent, feilutbetaling)
    }

    // Ignorterte ytelser: Alle uføre går under annet regelverk (samordning) og skal derfor ignoreres.
    val ignorerteYtelserOgUtbetalinger =
        listOf(
            "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere",
            "ufoeretrygd",
            "ufoereytelseEtteroppgjoer",
            "feriepenger",
            "ufoerepensjonFraAndreEnnFolketrygden",
        )
}

data class Inntektsendring(
    val fireMånederTilbake: BeregningResultat,
    val treMånederTilbake: BeregningResultat,
    val toMånederTilbake: BeregningResultat,
    val forrigeMåned: BeregningResultat,
) {
    fun harEndretInntekt() =
        fireMånederTilbake.prosent >= 10 && treMånederTilbake.prosent >= 10 && toMånederTilbake.prosent >= 10 && forrigeMåned.prosent >= 10
}

data class BeregningResultat(
    val beløp: Int,
    val prosent: Int,
    val feilutbetaling: Int,
)
