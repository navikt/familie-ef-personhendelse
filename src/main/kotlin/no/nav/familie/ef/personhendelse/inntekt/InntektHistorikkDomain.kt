package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.YearMonth

data class InntektshistorikkResponse(
    val aarMaanedHistorikk: Map<String, Map<String, List<InntektVersjon>>>? // <Year-month>, <orgnr, inntekt>
) {
    fun inntektForMåned(yearMonth: String) = aarMaanedHistorikk?.get(yearMonth)?.values?.flatten()
}

data class InntektVersjon(
    val arbeidsInntektInformasjon: ArbeidsInntekthistorikkInformasjon,
    val avvikListe: List<Avvik>?,
    val innleveringstidspunkt: String,
    val opplysningspliktig: String,
    val versjon: Int
)

data class ArbeidsInntekthistorikkInformasjon(
    val arbeidsforholdListe: List<ArbeidsforholdFrilanser>?,
    val forskuddstrekkListe: List<Forskuddstrekk>?,
    val fradragListe: List<Fradrag>?,
    val inntektListe: List<Inntekt>?
) {
    data class ArbeidsforholdFrilanser(
        val antallTimerPerUkeSomEnFullStillingTilsvarer: Double,
        val arbeidsforholdID: String,
        val arbeidsforholdIDnav: String,
        val arbeidsforholdstype: String,
        val arbeidsgiver: Aktør,
        val arbeidstaker: Aktør,
        val arbeidstidsordning: String,
        val avloenningstype: String,
        val frilansPeriodeFom: String,
        val frilansPeriodeTom: String,
        val sisteDatoForStillingsprosentendring: String,
        val sisteLoennsendring: String,
        val stillingsprosent: Double,
        val yrke: String
    )

    data class Forskuddstrekk(
        @JsonProperty("beloep")
        val beløp: Int,
        val beskrivelse: String,
        val forskuddstrekkGjelder: Aktør,
        val leveringstidspunkt: String,
        val opplysningspliktig: Aktør,
        val utbetaler: Aktør
    )

    data class Fradrag(
        @JsonProperty("beloep")
        val beløp: Int,
        val beskrivelse: String,
        val fradragGjelder: Aktør,
        val fradragsperiode: YearMonth,
        val inntektspliktig: Aktør,
        val leveringstidspunkt: String,
        val utbetaler: Aktør
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
        val virksomhet: Aktør
    )
}

data class TilleggsinformasjonDetaljer(
    val ikkebeskrevet: String?
)
