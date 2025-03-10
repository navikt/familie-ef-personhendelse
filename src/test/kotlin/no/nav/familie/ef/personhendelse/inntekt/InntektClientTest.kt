package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.personhendelse.inntekt.inntektv2.InntektResponse
import no.nav.familie.ef.personhendelse.inntekt.inntektv2.InntektTypeV2
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.YearMonth
import kotlin.test.assertEquals

class InntektClientTest {
    @Nested
    inner class ParseInntektRepsonse {
        @Test
        internal fun `parser generell inntekt response med riktig data struktur`() {
            val inntektV2ResponseJson: String = lesRessurs("inntekt/inntektv2/GenerellInntektV2Response.json")
            val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJson)

            val forventetMåned = YearMonth.of(2020, 3)
            val forventetInntektType: InntektTypeV2 = InntektTypeV2.LØNNSINNTEKT

            assertEquals(forventetMåned, inntektResponse.månedsData[0].måned)
            assertEquals(forventetInntektType, inntektResponse.månedsData[0].inntektListe[0].type)
        }

        @Test
        internal fun `parser inntektv2 response med forskjellige inntekt typer`() {
            val inntektV2ResponseJson: String = lesRessurs("inntekt/inntektv2/FlereInntektTyperInntektV2Response.json")
            val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJson)

            val forventeteInntektTyper =
                listOf(
                    InntektTypeV2.LØNNSINNTEKT,
                    InntektTypeV2.NAERINGSINNTEKT,
                    InntektTypeV2.YTELSE_FRA_OFFENTLIGE,
                    InntektTypeV2.PENSJON_ELLER_TRYGD,
                )

            val faktiskeInntektTyper =
                inntektResponse.månedsData
                    .flatMap { it.inntektListe }
                    .map { it.type }
                    .distinct()

            assertEquals(forventeteInntektTyper.sorted(), faktiskeInntektTyper.sorted())
        }
    }

    companion object {
        fun lesRessurs(name: String): String {
            val resource =
                this::class.java.classLoader.getResource(name)
                    ?: throw IllegalArgumentException("Resource not found: $name")
            return resource.readText(StandardCharsets.UTF_8)
        }
    }
}
