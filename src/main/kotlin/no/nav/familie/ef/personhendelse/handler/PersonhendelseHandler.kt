package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.Hendelse
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

        if (personhendelse.skalOpphøreEllerKorrigeres()) {
            opphørEllerKorrigerOppgave(personhendelse)
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
        lagreHendelse(personhendelse, oppgaveId)
        logger.info("Oppgave opprettet med oppgaveId=$oppgaveId")
    }

    private fun opphørEllerKorrigerOppgave(personhendelse: Personhendelse) {
        val hendelse = hentHendelse(personhendelse)
        if (hendelse == null) {
            logger.info("Tidligere hendelse for personhendelse : ${personhendelse.hendelseId} ble ikke funnet")
            val oppgaveId = opprettOppgaveMedBeskrivelse(personhendelse, personhendelse.finnesIngenHendelseBeskrivelse())
            logger.info("Oppgave for at det ikke finnes hendelse opprettet med oppgaveId=${oppgaveId}")
            return
        }
        val oppgave = hentOppgave(hendelse)
        if (oppgave.erÅpen()) {
            val nyOppgave = when (personhendelse.endringstype) {
                Endringstype.ANNULLERT -> oppdater(oppgave, oppgave.opphørtEllerAnnullertBeskrivelse(), StatusEnum.FEILREGISTRERT)
                Endringstype.OPPHOERT -> oppdater(oppgave, oppgave.opphørtEllerAnnullertBeskrivelse(), StatusEnum.FERDIGSTILT)
                Endringstype.KORRIGERT -> oppdater(oppgave, oppgave.korrigertBeskrivelse(), oppgave.status)
                else -> error("Feil endringstype ved annullering eller korrigering : ${personhendelse.endringstype}")
            }
            logger.info("Oppgave oppdatert med oppgaveId=${nyOppgave} for endringstype : ${personhendelse.endringstype.name}")
        } else {
            opprettOppgaveMedBeskrivelse(personhendelse, personhendelse.ferdigstiltBeskrivelse())
            logger.info("Ny oppgave ifm en allerede lukket oppgave er opprettet med oppgaveId=${oppgave.id}")
        }
        lagreHendelse(personhendelse, oppgave.id!!)
    }

    private fun oppdater(oppgave: Oppgave, beskrivelse: String, status: StatusEnum?): Long {
        val nyOppgave = oppgave.copy(beskrivelse = oppgave.beskrivelse.plus(beskrivelse), status = status)
        return oppgaveClient.oppdaterOppgave(nyOppgave)
    }

    private fun hentHendelse(personhendelse: Personhendelse): Hendelse? {
        return personhendelseRepository.hentHendelse(UUID.fromString(personhendelse.tidligereHendelseId))
    }

    private fun lagreHendelse(personhendelse: Personhendelse, oppgaveId: Long) {
        personhendelseRepository.lagrePersonhendelse(
            UUID.fromString(personhendelse.hendelseId),
            oppgaveId,
            personhendelse.endringstype
        )
    }

    private fun hentOppgave(hendelse: Hendelse): Oppgave {
        return oppgaveClient.finnOppgaveMedId(hendelse.oppgaveId)
    }

    private fun opprettOppgaveMedBeskrivelse(personhendelse: Personhendelse, beskrivelse: String): Long {
        return oppgaveClient.opprettOppgave(
            defaultOpprettOppgaveRequest(
                personhendelse.personidenter.first(),
                beskrivelse
            )
        )
    }
}

private fun Personhendelse.ferdigstiltBeskrivelse() =
    "En hendelse av typen ${this.endringstype.name} har oppstått for en ferdigstilt oppgave"

private fun Personhendelse.finnesIngenHendelseBeskrivelse() =
    "Det har oppstått en personhendelse som det ikke finnes noen tidligere hendelse eller oppgave for. " +
            "Personhendelse id : ${this.hendelseId}, ${this.endringstype.name}"

private fun Oppgave.erÅpen() = !listOf(StatusEnum.FERDIGSTILT, StatusEnum.FEILREGISTRERT).contains(this.status)

private fun Oppgave.opphørtEllerAnnullertBeskrivelse() = "\n\nDenne oppgaven har opphørt eller blitt annullert."

private fun Oppgave.korrigertBeskrivelse() = "\n\nDenne oppgaven har blitt korrigert."

private fun Personhendelse.skalOpphøreEllerKorrigeres() =
    listOf(Endringstype.ANNULLERT, Endringstype.KORRIGERT, Endringstype.OPPHOERT).contains(this.endringstype)