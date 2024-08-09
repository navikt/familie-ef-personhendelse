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
    private val grunnbeløp = 124_028
    private val halvtGrunnbeløpMånedlig = (grunnbeløp / 2) / 12
    private val maxInntekt = Math.floor((grunnbeløp * 5.5) / 1000L) * 1000L // Ingen utbetaling av OS ved inntekt på over 5.5 rundet ned til nærmeste 1000

    fun beregnEndretInntekt(
        inntektResponse: HentInntektListeResponse,
        forventetInntektForPerson: ForventetInntektForPerson,
    ): Inntektsendring {
        // hent alle registrerte vedtak som var på personen sist beregning
        val nyesteRegistrerteInntekt =
            inntektResponse.arbeidsinntektMåned?.filter { it.årMåned == YearMonth.now().minusMonths(1) }
        val nestNyesteRegistrerteInntekt =
            inntektResponse.arbeidsinntektMåned?.filter { it.årMåned == YearMonth.now().minusMonths(2) }
        val inntektTreMånederTilbake =
            inntektResponse.arbeidsinntektMåned?.filter { it.årMåned == YearMonth.now().minusMonths(3) }
        val inntektFireMånederTilbake =
            inntektResponse.arbeidsinntektMåned?.filter { it.årMåned == YearMonth.now().minusMonths(4) }

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
        nyesteRegistrerteInntekt: List<ArbeidsinntektMåned>?,
        ident: String,
        forventetInntekt: Int?,
    ): BeregningResultat {
        if (forventetInntekt == null ||
            nyesteRegistrerteInntekt.isNullOrEmpty() ||
            nyesteRegistrerteInntekt
                .firstOrNull()
                ?.arbeidsInntektInformasjon
                ?.inntektListe
                .isNullOrEmpty()
        ) {
            secureLogger.warn("Ingen gjeldende inntekt funnet på person $ident har personen løpende stønad?")
            return BeregningResultat(0, 0, 0)
        }

        if (forventetInntekt > maxInntekt) return BeregningResultat(0, 0, 0) // Ignorer alle med over 652000 i årsinntekt, da de har 0 i utbetaling.
        val månedligForventetInntekt = (forventetInntekt / 12)

        val inntektListe = nyesteRegistrerteInntekt.firstOrNull()?.arbeidsInntektInformasjon?.inntektListe ?: emptyList()
        val samletInntekt =
            inntektListe
                .filterNot {
                    ignorerteYtelserOgUtbetalinger.contains(it.beskrivelse)
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
        treMånederTilbake.prosent >= 10 && toMånederTilbake.prosent >= 10 && forrigeMåned.prosent >= 10
}

data class BeregningResultat(
    val beløp: Int,
    val prosent: Int,
    val feilutbetaling: Int,
)
