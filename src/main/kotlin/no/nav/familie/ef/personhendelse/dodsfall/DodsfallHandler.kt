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
class DodsfallHandler(val sakClient: SakClient, val oppgaveClient: OppgaveClient) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun handleDodsfallHendelse(personhendelse: Personhendelse) {
        val personIdent = personhendelse.personidenter.map { it.toString() }.first()

        //TODO: Lag PDL-client og hent foreldre (for å sjekke om de mottar stønad)
        val finnesBehandlingForPerson = sakClient.finnesBehandlingForPerson(personIdent, StønadType.OVERGANGSSTØNAD)
        secureLogger.info("Finnes behandling for person: $finnesBehandlingForPerson")
        logger.info("Finnes behandling for person: $finnesBehandlingForPerson")
        if (finnesBehandlingForPerson) {
            val opprettOppgaveRequest =
                OpprettOppgaveRequest(
                    ident = OppgaveIdentV2(ident = personIdent, gruppe = IdentGruppe.FOLKEREGISTERIDENT),
                    saksId = null,
                    tema = Tema.ENF,
                    oppgavetype = Oppgavetype.VurderHenvendelse,
                    fristFerdigstillelse = LocalDate.now(),
                    beskrivelse = "Saken ligger i ny løsning. Opprettet som følge av personhendelse",
                    enhetsnummer = null,
                    behandlingstema = Behandlingstema.Overgangsstønad.value,
                    tilordnetRessurs = null,
                    behandlesAvApplikasjon = "familie-ef-sak"
                )
            oppgaveClient.opprettOppgave(opprettOppgaveRequest)
        }
        // er personen stønadsmottaker: opprett oppgave
        // sjekk om foreldre er stønadsmottaker: Opprett oppgave
    }
}