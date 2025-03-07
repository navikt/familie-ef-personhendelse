package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.YearMonth

data class InntektshistorikkResponse(
    val aarMaanedHistorikk: Map<YearMonth, Map<String, List<InntektVersjon>>> = emptyMap(), // <Year-month>, <orgnr, inntekt>
) {
    fun inntektForMåned(yearMonth: YearMonth) = aarMaanedHistorikk[yearMonth]?.values?.flatten() ?: emptyList()

    fun inntektEntryForMåned(yearMonth: YearMonth) = aarMaanedHistorikk[yearMonth] ?: emptyMap()
}

data class InntektVersjon(
    val arbeidsInntektInformasjon: ArbeidsInntekthistorikkInformasjon,
    val avvikListe: List<Avvik>?,
    val innleveringstidspunkt: String?,
    val opplysningspliktig: String,
    val versjon: Int,
)

data class ArbeidsInntekthistorikkInformasjon(
    val arbeidsforholdListe: List<ArbeidsforholdFrilanser>?,
    val forskuddstrekkListe: List<Forskuddstrekk>?,
    val fradragListe: List<Fradrag>?,
    val inntektListe: List<Inntekt>?,
) {
    data class ArbeidsforholdFrilanser(
        val antallTimerPerUkeSomEnFullStillingTilsvarer: Double?,
        val arbeidsforholdID: String?,
        val arbeidsforholdIDnav: String?,
        val arbeidsforholdstype: String?,
        val arbeidsgiver: Aktør?,
        val arbeidstaker: Aktør?,
        val arbeidstidsordning: String?,
        val avloenningstype: String?,
        val frilansPeriodeFom: String?,
        val frilansPeriodeTom: String?,
        val sisteDatoForStillingsprosentendring: String?,
        val sisteLoennsendring: String?,
        val stillingsprosent: Double?,
        val yrke: String?,
    )

    data class Forskuddstrekk(
        @JsonProperty("beloep")
        val beløp: Int?,
        val beskrivelse: String?,
        val forskuddstrekkGjelder: Aktør?,
        val leveringstidspunkt: String?,
        val opplysningspliktig: Aktør?,
        val utbetaler: Aktør?,
    )

    data class Fradrag(
        @JsonProperty("beloep")
        val beløp: Int?,
        val beskrivelse: String?,
        val fradragGjelder: Aktør?,
        val fradragsperiode: YearMonth?,
        val inntektspliktig: Aktør?,
        val leveringstidspunkt: String?,
        val utbetaler: Aktør?,
    )

    data class Inntekt(
        val antall: Int?,
        val arbeidsforholdREF: String?,
        @JsonProperty("beloep")
        val beløp: Int,
        val beskrivelse: String,
        val fordel: String,
        val informasjonsstatus: String,
        val inngaarIGrunnlagForTrekk: Boolean,
        val inntektType: InntektType,
        val inntektskilde: String,
        val inntektsmottaker: Aktør,
        val inntektsperiodetype: String,
        val inntektsstatus: String,
        val leveringstidspunkt: YearMonth,
        val opplysningspliktig: Aktør,
        val opptjeningsland: String?,
        val opptjeningsperiodeFom: String?,
        val opptjeningsperiodeTom: String?,
        val skatteOgAvgiftsregel: String?,
        val skattemessigBosattLand: String?,
        val tilleggsinformasjon: Tilleggsinformasjon?,
        val utbetaltIMaaned: YearMonth,
        val utloeserArbeidsgiveravgift: Boolean,
        val virksomhet: Aktør,
    )
}

data class TilleggsinformasjonDetaljer(
    val ikkebeskrevet: String?,
    val detaljerType: String?,
)

data class Avvik(
    val ident: Aktør? = null,
    val opplysningspliktig: Aktør? = null,
    val virksomhet: Aktør,
    val avvikPeriode: YearMonth? = null,
    val tekst: String? = null,
)

data class Aktør(
    val identifikator: String,
    @JsonProperty("aktoerType")
    val aktørType: AktørType,
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

data class Tilleggsinformasjon(
    val kategori: String? = null, // Kodeverk -> EDAGTilleggsinfoKategorier
    val tilleggsinformasjonDetaljer: TilleggsinformasjonDetaljer?,
)
