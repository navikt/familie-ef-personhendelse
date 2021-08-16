package no.nav.familie.ef.personhendelse.dodsfall

import no.nav.familie.ef.personhendelse.oppgave.OppgaveClient
import no.nav.familie.ef.personhendelse.sak.SakClient
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

    @Transactional
    fun handleDodsfallHendelse(personhendelse: Personhendelse) {
        val personIdent = personhendelse.personidenter.map { it.toString() }.first()

        val finnesBehandlingForPerson = sakClient.finnesBehandlingForPerson(personIdent, StønadType.OVERGANGSSTØNAD)
        secureLogger.info("Finnes behandling med personIdent: $personIdent : $finnesBehandlingForPerson")
        if (finnesBehandlingForPerson) {
            val opprettOppgaveRequest =
                OpprettOppgaveRequest(
                    ident = OppgaveIdentV2(ident = personIdent, gruppe = IdentGruppe.FOLKEREGISTERIDENT),
                    saksId = null,
                    tema = Tema.ENF,
                    oppgavetype = Oppgavetype.VurderLivshendelse,
                    fristFerdigstillelse = LocalDate.now(),
                    beskrivelse = "Opprettet som følge av personhendelse av type dødsfall",
                    enhetsnummer = null,
                    behandlingstema = Behandlingstema.Overgangsstønad.value,
                    tilordnetRessurs = null,
                    behandlesAvApplikasjon = "familie-ef-sak"
                )
            val oppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest)
            secureLogger.info("Oppgave opprettet med oppgaveId: $oppgaveId")
        }
    }
}