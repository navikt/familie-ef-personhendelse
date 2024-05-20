package no.nav.familie.ef.personhendelse.kontantstøtte.vedtak

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.lagVurderKonsekvensoppgaveForBarnetilsyn
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class KontantstøtteVedtakService(
    private val efSakClient: SakClient,
    private val oppgaveClient: OppgaveClient,
    private val kontantstøtteVedtakRepository: KontantstøtteVedtakRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun harLøpendeBarnetilsyn(personIdent: String): Boolean {
        return efSakClient.harLøpendeBarnetilsyn(personIdent)
    }

    fun opprettVurderKonsekvensOppgaveForBarnetilsyn(
        personIdent: String,
        beskrivelse: String,
    ) {
        val opprettOppgaveRequest =
            lagVurderKonsekvensoppgaveForBarnetilsyn(
                personIdent = personIdent,
                beskrivelse = beskrivelse,
            )
        val oppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest)
        oppgaveClient.leggOppgaveIMappe(oppgaveId)
        logger.info("Oppgave for kontantstøttevedtak opprettet med oppgaveId=$oppgaveId")
    }

    fun lagreKontantstøttehendelse(behandlingId: String) {
        kontantstøtteVedtakRepository.lagreKontantstøttevedtak(behandlingId)
    }

    fun erAlleredeHåndtert(behandlingId: String): Boolean {
        return kontantstøtteVedtakRepository.harAlleredeProsessertKontantstøttevedtak(behandlingId)
    }
}
