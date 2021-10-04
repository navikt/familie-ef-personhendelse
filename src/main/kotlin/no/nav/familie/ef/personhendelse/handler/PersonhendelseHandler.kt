package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.defaultOpprettOppgaveRequest
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

abstract class PersonhendelseHandler(
    val sakClient: SakClient,
    val oppgaveClient: OppgaveClient,
    val personhendelseRepository: PersonhendelseRepository
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Suppress("MemberVisibilityCanBePrivate")
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    abstract val type: PersonhendelseType

    open fun skalOppretteOppgave(personhendelse: Personhendelse) = true

    abstract fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): String

    open fun handle(personhendelse: Personhendelse) {
        if (personhendelse.erAnnulleringEllerKorreksjon()) {
            handleAnnulleringEllerKorreksjon(personhendelse)
            return
        }
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

    private fun logHendelse(
        personhendelse: Personhendelse,
        skalOppretteOppgave: Boolean,
        personIdent: String?
    ) {
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
        personhendelseRepository.lagrePersonhendelse(personhendelse.hendelseId, oppgaveId, personhendelse.endringstype)
        logger.info("Oppgave opprettet med oppgaveId=$oppgaveId")
    }

    private fun handleAnnulleringEllerKorreksjon(personhendelse: Personhendelse) {
        val hendelse = personhendelseRepository.hentHendelse(UUID.fromString(personhendelse.hendelseId)) ?: return
        val oppgave = oppgaveClient.finnOppgaveMedId(hendelse.oppgaveId) ?: return
        if (oppgave.erOppgaveÅpen()) {
            ferdigstillOppgave(oppgave)
        } else {
            oppgaveClient.opprettOppgave(
                defaultOpprettOppgaveRequest(
                    personhendelse.personidenter.first(),
                    personhendelse.ferdigstiltBeskrivelse()
                )
            )
        }
    }

    private fun ferdigstillOppgave(oppgave: Oppgave): Oppgave {
        val nyOppgave = oppgave.copy(
            beskrivelse = oppgave.beskrivelse.plus(oppgave.annullertEllerKorrigertBeskrivelse()),
            status = StatusEnum.FEILREGISTRERT
        )
        return oppgaveClient.oppdaterOppgave(nyOppgave)
    }

}

private fun Personhendelse.ferdigstiltBeskrivelse() =
    "En hendelse av typen ${this.endringstype.name} har oppstått for en ferdigstilt oppgave"

private fun Oppgave.erOppgaveÅpen() = !listOf(StatusEnum.FERDIGSTILT, StatusEnum.FEILREGISTRERT).contains(this.status)

private fun Oppgave.annullertEllerKorrigertBeskrivelse() = "\n\nDenne oppgaven har blitt annullert eller korrigert."

private fun Personhendelse.erAnnulleringEllerKorreksjon() =
    listOf(Endringstype.ANNULLERT, Endringstype.KORRIGERT).contains(this.endringstype)