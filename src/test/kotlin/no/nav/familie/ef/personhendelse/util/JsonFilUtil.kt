package no.nav.familie.ef.personhendelse.util

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.personhendelse.inntekt.InntektResponse
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions
import java.nio.charset.StandardCharsets
import java.time.YearMonth

class JsonFilUtil {
    companion object {
        fun readResource(name: String): String =
            this::class.java.classLoader
                .getResource(name)!!
                .readText(StandardCharsets.UTF_8)

        fun lagInntektsResponseFraJsonMedEnMåned(json: String): InntektResponse {
            val enMndTilbake = YearMonth.now().minusMonths(1)
            val toMndTilbake = YearMonth.now().minusMonths(2)
            val treMndTilbake = YearMonth.now().minusMonths(3)
            val fireMndTilbake = YearMonth.now().minusMonths(4)

            val inntektResponse = objectMapper.readValue<InntektResponse>(json)
            val månedsInntekt = inntektResponse.inntektsmåneder.firstOrNull() ?: Assertions.fail("Inntekt mangler")

            return inntektResponse.copy(
                inntektsmåneder =
                    listOf(
                        månedsInntekt.copy(måned = enMndTilbake),
                        månedsInntekt.copy(måned = toMndTilbake),
                        månedsInntekt.copy(måned = treMndTilbake),
                        månedsInntekt.copy(måned = fireMndTilbake),
                    ),
            )
        }

        fun lagInntektsResponseFraToJsonsMedEnMåned(
            jsonForResterendeMåneder: String,
            jsonForFørsteMåned: String,
        ): InntektResponse {
            val enMndTilbake = YearMonth.now().minusMonths(1)
            val toMndTilbake = YearMonth.now().minusMonths(2)
            val treMndTilbake = YearMonth.now().minusMonths(3)
            val fireMndTilbake = YearMonth.now().minusMonths(4)

            val inntektResponseResterendeMåneder = objectMapper.readValue<InntektResponse>(jsonForResterendeMåneder)
            val inntektResponseFørsteMåned = objectMapper.readValue<InntektResponse>(jsonForFørsteMåned)

            val arbeidsinntektResterendeMåneder = inntektResponseResterendeMåneder.inntektsmåneder.firstOrNull() ?: Assertions.fail("Inntekt mangler")
            val arbeidsinntektFørsteMåned = inntektResponseFørsteMåned.inntektsmåneder.firstOrNull() ?: Assertions.fail("Inntekt mangler")

            return inntektResponseResterendeMåneder.copy(
                inntektsmåneder =
                    listOf(
                        arbeidsinntektFørsteMåned.copy(måned = enMndTilbake),
                        arbeidsinntektResterendeMåneder.copy(måned = toMndTilbake),
                        arbeidsinntektResterendeMåneder.copy(måned = treMndTilbake),
                        arbeidsinntektResterendeMåneder.copy(måned = fireMndTilbake),
                    ),
            )
        }
    }
}
