package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.defaultOpprettOppgaveRequest
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
abstract class PersonhendelseHandler(
        val sakClient: SakClient,
        val oppgaveClient: OppgaveClient
) {

    final val logger = LoggerFactory.getLogger(this::class.java)
    final val secureLogger = LoggerFactory.getLogger("secureLogger")

    abstract val type: String

    fun handle(personhendelse: Personhendelse) {
        val personIdent = personhendelse.personidenter.first() // todo endre til att sakClient kan ta emot en liste med identer
        val finnesBehandlingForPerson = sakClient.finnesBehandlingForPerson(personIdent)

        if (finnesBehandlingForPerson) {
            handlePersonhendelse(personhendelse, personIdent)
        }
    }

    final fun handlePersonhendelse(personhendelse: Personhendelse, personIdent: String) {
        val skalOppretteOppgave = skalOppretteOppgave()
        logHendelse(personhendelse, skalOppretteOppgave, personIdent)

        if (!skalOppretteOppgave) {
            return
        }
        opprettOppgave(personhendelse, personIdent)
    }

    private fun logHendelse(personhendelse: Personhendelse,
                            skalOppretteOppgave: Boolean,
                            personIdent: String?) {
        val logMessage = "Finnes sak for opplysningstype=${personhendelse.opplysningstype}" +
                         " hendelseId=${personhendelse.hendelseId}" +
                         " endringstype=${personhendelse.endringstype}" +
                         " skalOppretteOppgave=$skalOppretteOppgave"
        logger.info(logMessage)
        secureLogger.info("$logMessage personIdent=${personIdent}")
    }

    private fun opprettOppgave(personhendelse: Personhendelse, personIdent: String) {
        val oppgaveBeskrivelse = lagOppgaveBeskrivelse(personhendelse)
        val opprettOppgaveRequest = defaultOpprettOppgaveRequest(personIdent, oppgaveBeskrivelse)
        val oppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest)
        logger.info("Oppgave opprettet med oppgaveId=$oppgaveId")
    }

    abstract fun skalOppretteOppgave(): Boolean

    abstract fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): String

}