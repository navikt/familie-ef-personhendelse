package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.defaultOpprettOppgaveRequest
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class DodsfallHandler(
    val sakClient: SakClient,
    val oppgaveClient: OppgaveClient
) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun handleDodsfall(personhendelse: Personhendelse) {
        val personIdent = personhendelse.personidenter.map { it.toString() }.first()

        val finnesBehandlingForPerson = sakClient.finnesBehandlingForPerson(personIdent, StønadType.OVERGANGSSTØNAD)
        secureLogger.info("Finnes behandling med personIdent: $personIdent : $finnesBehandlingForPerson")
        if (finnesBehandlingForPerson) {
            val opprettOppgaveRequest = defaultOpprettOppgaveRequest(personIdent, "dødsfall med dødsdato: ${personhendelse.doedsfall.doedsdato}" )
            val oppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest)
            secureLogger.info("Oppgave opprettet med oppgaveId: $oppgaveId")
        }
    }
}