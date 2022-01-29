package no.nav.familie.ef.personhendelse.inntekt.vedtak

import no.nav.familie.kontrakter.felles.ef.StønadType
import java.time.YearMonth

data class VedtakhendelseInntektberegning(
    val behandlingId: Long,
    val personIdent: String,
    val stønadType: StønadType,
    val aarMaanedProsessert: YearMonth,
    val versjon: Int
)
