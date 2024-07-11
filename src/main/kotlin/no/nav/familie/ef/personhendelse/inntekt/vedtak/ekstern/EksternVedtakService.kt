package no.nav.familie.ef.personhendelse.inntekt.vedtak.ekstern

import no.nav.familie.ef.personhendelse.client.SakClient
import org.springframework.stereotype.Service

@Service
class EksternVedtakService(
    val sakClient: SakClient,
) {
    fun mottarEfStønad(vedtakhendelse: Vedtakhendelse): Boolean = sakClient.harLøpendeStønad(setOf(vedtakhendelse.fødselsnummer))
}
