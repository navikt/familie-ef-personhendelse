package no.nav.familie.ef.personhendelse.inntekt.inntektv2

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth

data class InntektV2RequestBody(
    val maanedFom: YearMonth?,
    val maanedTom: YearMonth?,
)

data class InntektV2Response(
    @JsonProperty("data")
    val maanedsData: List<MånedsInntekt> = emptyList(),
)

data class MånedsInntekt(
    @JsonProperty("maaned")
    val måned: YearMonth,
    val opplysningspliktig: String,
    val underenhet: String,
    val norskident: String,
    @JsonProperty("oppsummeringstidspunkt")
    val oppsummeringsTidspunkt: OffsetDateTime,
    val inntektListe: List<Inntekt> = emptyList(),
    val forskuddstrekkListe: List<Forskuddstrekk> = emptyList(),
    val avvikListe: List<Avvik> = emptyList(),
)

data class Inntekt(
    val type: InntektTypeV2,
    @JsonProperty("beloep")
    val beløp: Double,
    val fordel: String,
    val beskrivelse: String,
    val inngaarIGrunnlagForTrekk: Boolean,
    @JsonProperty("utloeserArbeidsgiveravgift")
    val utløserArbeidsgiveravgift: Boolean,
    val skatteOgAvgiftsregel: String?,
    val opptjeningsperiodeFom: LocalDate?,
    val opptjeningsperiodeTom: LocalDate?,
    val tilleggsinformasjon: Tilleggsinformasjon?,
    val manuellVurdering: Boolean,
    val antall: Int?,
    val skattemessigBosattLand: String?,
    val opptjeningsland: String?,
)

data class Forskuddstrekk(
    @JsonProperty("beloep")
    val beløp: Double,
    val beskrivelse: String?,
)

data class Avvik(
    val kode: String,
    val tekst: String?,
)

data class Tilleggsinformasjon(
    val type: String,
)

// TODO: Husk å endre tilbake til normalt navn, samt fjerne bruken et at annet sted.
enum class InntektTypeV2 {
    @JsonProperty("Loennsinntekt")
    LØNNSINNTEKT,

    @JsonProperty("Naeringsinntekt")
    NAERINGSINNTEKT,

    @JsonProperty("PensjonEllerTrygd")
    PENSJON_ELLER_TRYGD,

    @JsonProperty("YtelseFraOffentlige")
    YTELSE_FRA_OFFENTLIGE,
}

fun InntektV2Response.oppsummerInntektForÅr(år: Int): Double =
    this.maanedsData
        .filter { it.måned.year == år }
        .flatMap { it.inntektListe }
        .sumOf { it.beløp }

fun List<Inntekt>.filterBasertPåInntektType(inntektType: InntektTypeV2): List<Inntekt> = this.filter { it.type == inntektType }

fun List<MånedsInntekt>.summerTotalInntekt(): Double = this.flatMap { it.inntektListe }.sumOf { it.beløp }
