package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.YearMonth

data class HentInntektListeResponse(
    @JsonProperty("arbeidsInntektMaaned")
    val arbeidsinntektMåned: List<ArbeidsinntektMåned>?,
    val ident: Aktør,
)

data class ArbeidsinntektMåned(
    @JsonProperty("aarMaaned")
    val årMåned: YearMonth, // rapportert for den måneden
    val avvikListe: List<Avvik>?,
    val arbeidsInntektInformasjon: ArbeidsInntektInformasjon?,
)

data class Aktør(
    val identifikator: String,
    @JsonProperty("aktoerType")
    val aktørType: AktørType,
)

data class Avvik(
    val ident: Aktør? = null,
    val opplysningspliktig: Aktør? = null,
    val virksomhet: Aktør,
    val avvikPeriode: YearMonth? = null,
    val tekst: String? = null,
)

data class ArbeidsInntektInformasjon(
    val inntektListe: List<AMeldingInntekt>?,
)

data class AMeldingInntekt(
    val inntektType: InntektType,
    @JsonProperty("beloep")
    val beløp: Int,
    val fordel: String, // kontantytese / etc
    val opptjeningsland: String? = null,
    val opptjeningsperiodeFom: LocalDate? = null,
    val opptjeningsperiodeTom: LocalDate? = null,
    val skattemessigBosattLand: String? = null,
    val virksomhet: Aktør, // ? = null,
    val tilleggsinformasjon: Tilleggsinformasjon? = null,
    val beskrivelse: String? = null, // hentes fra kodeverk
)

data class Tilleggsinformasjon(
    val kategori: String? = null, // Kodeverk -> EDAGTilleggsinfoKategorier
    val tilleggsinformasjonDetaljer: TilleggsinformasjonDetaljer?,
)

enum class AktørType {
    AKTOER_ID,
    NATURLIG_IDENT,
    ORGANISASJON,
}

enum class InntektType {
    LOENNSINNTEKT,
    NAERINGSINNTEKT,
    PENSJON_ELLER_TRYGD,
    YTELSE_FRA_OFFENTLIGE,
}

fun List<ArbeidsinntektMåned>.summerTotalInntekt(): Double =
    this
        .flatMap { it.arbeidsInntektInformasjon?.inntektListe ?: emptyList() }
        .sumOf { it.beløp.toDouble() }
