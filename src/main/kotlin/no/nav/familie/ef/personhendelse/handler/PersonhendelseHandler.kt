package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.defaultOpprettOppgaveRequest
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class PersonhendelseHandler(
        val sakClient: SakClient,
        val oppgaveClient: OppgaveClient,
        val personhendelseRepository: PersonhendelseRepository
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Suppress("MemberVisibilityCanBePrivate")
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    abstract val type: PersonhendelseType

    open fun handle(personhendelse: Personhendelse) {
        val personIdent = personhendelse.personidenter.first() // todo endre til att sakClient kan ta emot en liste med identer
        // TODO fjern stønadtype og returner stønadtype og resultat fra sakClient
        val finnesBehandlingForPerson = sakClient.finnesBehandlingForPerson(personIdent, StønadType.OVERGANGSSTØNAD)

        if (finnesBehandlingForPerson) {
            handlePersonhendelse(personhendelse, personIdent)
        }
    }

    fun handlePersonhendelse(personhendelse: Personhendelse, personIdent: String) {
        val skalOppretteOppgave = skalOppretteOppgave(personhendelse)
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
        val opprettOppgaveRequest = defaultOpprettOppgaveRequest(personIdent, "Personhendelse: $oppgaveBeskrivelse")
        val oppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest)
        oppgaveId?.let {
            personhendelseRepository.lagrePersonhendelse(personhendelse.hendelseId, oppgaveId, personhendelse.endringstype)
        }
        logger.info("Oppgave opprettet med oppgaveId=$oppgaveId")
    }

    open fun skalOppretteOppgave(personhendelse: Personhendelse) = true

    abstract fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): String

}