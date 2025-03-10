package no.nav.familie.ef.personhendelse.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.personhendelse.inntekt.inntektv2.InntektResponse
import no.nav.familie.ef.personhendelse.inntekt.inntektv2.InntektType
import no.nav.familie.kontrakter.felles.objectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.YearMonth

class InntektClientTest {
    @Nested
    inner class ParseInntektRepsonse {
        @Test
        fun `parser generell inntekt response med riktig data struktur`() {
            val inntektV2ResponseJson: String = lesRessurs("inntekt/InntektGenerellResponse.json")
            val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJson)

            val forventetMåned = YearMonth.of(2020, 3)
            val forventetInntektType: InntektType = InntektType.LØNNSINNTEKT

            Assertions.assertEquals(forventetMåned, inntektResponse.månedsData[0].måned)
            Assertions.assertEquals(forventetInntektType, inntektResponse.månedsData[0].inntektListe[0].type)
        }

        @Test
        fun `parser inntektv2 response med forskjellige inntekt typer`() {
            val inntektV2ResponseJson: String = lesRessurs("inntekt/InntektFlereInntektTyper.json")
            val inntektResponse = objectMapper.readValue<InntektResponse>(inntektV2ResponseJson)

            val forventeteInntektTyper =
                listOf(
                    InntektType.LØNNSINNTEKT,
                    InntektType.NAERINGSINNTEKT,
                    InntektType.YTELSE_FRA_OFFENTLIGE,
                    InntektType.PENSJON_ELLER_TRYGD,
                )

            val faktiskeInntektTyper =
                inntektResponse.månedsData
                    .flatMap { it.inntektListe }
                    .map { it.type }
                    .distinct()

            Assertions.assertEquals(forventeteInntektTyper.sorted(), faktiskeInntektTyper.sorted())
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
