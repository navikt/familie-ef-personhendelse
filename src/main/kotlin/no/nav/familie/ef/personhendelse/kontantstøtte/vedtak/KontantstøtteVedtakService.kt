package no.nav.familie.ef.personhendelse.kontantstøtte.vedtak

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.opprettVurderKonsekvensoppgaveForBarnetilsyn
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class KontantstøtteVedtakService(val efSakClient: SakClient, val oppgaveClient: OppgaveClient) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun harLøpendeBarnetilsyn(personIdent: String): Boolean {
        return efSakClient.harLøpendeBarnetilsyn(personIdent)
    }

    fun opprettVurderKonsekvensOppgaveForBarnetilsyn(personIdent: String, beskrivelse: String) {
        val opprettOppgaveRequest = opprettVurderKonsekvensoppgaveForBarnetilsyn(
            personIdent = personIdent,
            beskrivelse = beskrivelse,
        )
        val oppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest)
        oppgaveClient.leggOppgaveIMappe(oppgaveId)
        logger.info("Oppgave for kontantstøttevedtak opprettet med oppgaveId=$oppgaveId")
    }
}
