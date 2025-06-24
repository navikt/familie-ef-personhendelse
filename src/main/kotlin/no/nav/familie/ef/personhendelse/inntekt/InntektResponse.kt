package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.familie.ef.personhendelse.datoutil.isEqualOrAfter
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth

data class InntektResponse(
    @JsonProperty("data")
    val inntektsmåneder: List<Inntektsmåned> = emptyList(),
) {
    fun inntektsmånederFraOgMedÅrMåned(fraOgMedÅrMåned: YearMonth? = null): List<Inntektsmåned> =
        inntektsmåneder
            .filter { inntektsmåned ->
                inntektsmåned.måned.isBefore(YearMonth.now()) &&
                    inntektsmåned.måned.isEqualOrAfter(fraOgMedÅrMåned)
            }.sortedBy { it.måned }

    fun totalInntektForÅrMånedUtenFeriepenger(årMåned: YearMonth): Int =
        inntektsmånederFraOgMedÅrMåned(årMåned)
            .filter { it.måned == årMåned }
            .flatMap { it.inntektListe }
            .filter { it.beskrivelse != "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere" && it.beskrivelse != "barnepensjon" && !it.beskrivelse.contains("ferie", true) }
            .sumOf { it.beløp }
            .toInt()
}

data class Inntektsmåned(
    @JsonProperty("maaned")
    val måned: YearMonth,
    val opplysningspliktig: String,
    val underenhet: String,
    val norskident: String,
    val oppsummeringstidspunkt: OffsetDateTime,
    val inntektListe: List<Inntekt> = emptyList(),
    val forskuddstrekkListe: List<Forskuddstrekk> = emptyList(),
    val avvikListe: List<Avvik> = emptyList(),
)

data class Inntekt(
    val type: InntektType,
    @JsonProperty("beloep")
    val beløp: Double,
    val fordel: String,
    val beskrivelse: String,
    @JsonProperty("inngaarIGrunnlagForTrekk")
    val inngårIGrunnlagForTrekk: Boolean,
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

enum class InntektType {
    @JsonProperty("Loennsinntekt")
    LØNNSINNTEKT,

    @JsonProperty("Naeringsinntekt")
    NAERINGSINNTEKT,

    @JsonProperty("PensjonEllerTrygd")
    PENSJON_ELLER_TRYGD,

    @JsonProperty("YtelseFraOffentlige")
    YTELSE_FRA_OFFENTLIGE,
}
