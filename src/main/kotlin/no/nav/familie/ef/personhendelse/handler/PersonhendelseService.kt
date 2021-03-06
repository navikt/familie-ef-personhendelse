package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.Hendelse
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.defaultOpprettOppgaveRequest
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.familie.kontrakter.felles.oppgave.FinnMappeRequest
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PersonhendelseService(
    personhendelseHandlers: List<PersonhendelseHandler>,
    private val sakClient: SakClient,
    private val oppgaveClient: OppgaveClient,
    private val personhendelseRepository: PersonhendelseRepository
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    private val EF_ENHETNUMMER = "4489"
    private val handlers: Map<String, PersonhendelseHandler> = personhendelseHandlers.associateBy { it.type.hendelsetype }

    init {
        logger.info("Legger til handlers: {}", personhendelseHandlers)
        if (personhendelseHandlers.isEmpty()) {
            error("Finner ikke handlers for personhendelse")
        }
    }

    fun h√•ndterPersonhendelse(personhendelse: Personhendelse) {
        handlers[personhendelse.opplysningstype]?.let { handler ->
            handler.personidenterPerPersonSomSkalKontrolleres(personhendelse).forEach { personidenter ->
                if (personidenter.isEmpty()) {
                    error("Savner personidenter til personhendelse=${personhendelse.hendelseId}")
                }
                handle(handler, personhendelse, personidenter)
            }
        }
    }

    private fun handle(handler: PersonhendelseHandler, personhendelse: Personhendelse, personidenter: Set<String>) {
        val harL√łpendeSt√łnad = sakClient.harL√łpendeSt√łnad(personidenter)
        logger.info(
            "Personhendelse av opplysningstype=${personhendelse.opplysningstype} av type=${personhendelse.endringstype.name} - " +
                " harL√łpendeSt√łnad=$harL√łpendeSt√łnad"
        )
        if (harL√łpendeSt√łnad) {
            handlePersonhendelse(handler, personhendelse, personidenter.first())
        }
    }

    private fun handlePersonhendelse(handler: PersonhendelseHandler, personhendelse: Personhendelse, personIdent: String) {
        if (personhendelse.skalOpph√łreEllerKorrigeres()) {
            opph√łrEllerKorrigerOppgave(personhendelse)
            return
        }
        val oppgaveBeskrivelse = handler.lagOppgaveBeskrivelse(personhendelse)
        logHendelse(personhendelse, oppgaveBeskrivelse, personIdent)
        when (oppgaveBeskrivelse) {
            is IkkeOpprettOppgave -> return
            is OpprettOppgave -> opprettOppgave(oppgaveBeskrivelse, personhendelse, personIdent)
        }
    }

    private fun logHendelse(
        personhendelse: Personhendelse,
        oppgaveBeskrivelse: OppgaveInformasjon,
        personIdent: String?
    ) {
        val logMessage = "Finnes sak for opplysningstype=${personhendelse.opplysningstype}" +
            " hendelseId=${personhendelse.hendelseId}" +
            " endringstype=${personhendelse.endringstype}" +
            " skalOppretteOppgave=${oppgaveBeskrivelse is OpprettOppgave}"
        logger.info(logMessage)
        secureLogger.info("$logMessage personIdent=$personIdent")
    }

    private fun opprettOppgave(oppgaveBeskrivelse: OpprettOppgave, personhendelse: Personhendelse, personIdent: String) {
        val beskrivelse = oppgaveBeskrivelse.beskrivelse
        val opprettOppgaveRequest = defaultOpprettOppgaveRequest(personIdent, "Personhendelse: $beskrivelse")
        val oppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest)

        try {
            leggOppgaveIMappe(oppgaveId)
        } catch (e: Exception) {
            logger.error("Feil under knytning av mappe til oppgave - se securelogs for stacktrace")
            secureLogger.error("Feil under knytning av mappe til oppgave", e)
        }

        lagreHendelse(personhendelse, oppgaveId)
        logger.info("Oppgave opprettet med oppgaveId=$oppgaveId")
    }

    private fun opph√łrEllerKorrigerOppgave(personhendelse: Personhendelse) {
        val hendelse = hentHendelse(personhendelse)
        if (hendelse == null) {
            logger.warn(
                "Tidligere hendelse for personhendelse : ${personhendelse.hendelseId} ble ikke funnet. " +
                    personhendelse.finnesIngenHendelseBeskrivelse()
            )
            return
        }
        val oppgave = hentOppgave(hendelse)
        if (oppgave.er√Öpen()) {
            val nyOppgave = when (personhendelse.endringstype) {
                Endringstype.ANNULLERT -> oppdater(oppgave, oppgave.opph√łrtEllerAnnullertBeskrivelse(), StatusEnum.FEILREGISTRERT)
                Endringstype.OPPHOERT -> oppdater(oppgave, oppgave.opph√łrtEllerAnnullertBeskrivelse(), StatusEnum.FERDIGSTILT)
                Endringstype.KORRIGERT -> oppdater(oppgave, oppgave.korrigertBeskrivelse(), oppgave.status)
                else -> error("Feil endringstype ved annullering eller korrigering : ${personhendelse.endringstype}")
            }
            logger.info("Oppgave oppdatert med oppgaveId=$nyOppgave for endringstype : ${personhendelse.endringstype.name}")
        } else {
            opprettOppgaveMedBeskrivelse(personhendelse, personhendelse.ferdigstiltBeskrivelse())
            logger.info("Ny oppgave ifm en allerede lukket oppgave er opprettet med oppgaveId=${oppgave.id}")
        }
        lagreHendelse(personhendelse, oppgave.id!!)
    }

    fun leggOppgaveIMappe(oppgaveId: Long) {
        val oppgave = oppgaveClient.finnOppgaveMedId(oppgaveId)
        if (oppgave.tildeltEnhetsnr == EF_ENHETNUMMER) { // Skjermede personer skal ikke puttes i mappe
            val finnMappeRequest = FinnMappeRequest(
                listOf(),
                oppgave.tildeltEnhetsnr!!,
                null,
                1000
            )
            val mapperResponse = oppgaveClient.finnMapper(finnMappeRequest)
            val mappe = mapperResponse.mapper.find {
                it.navn.contains("EF Sak", true) &&
                    it.navn.contains("Hendelser") &&
                    it.navn.contains("62")
            }
                ?: error("Fant ikke mappe for uplassert oppgave (EF Sak og 01)")
            oppgaveClient.oppdaterOppgave(oppgave.copy(mappeId = mappe.id.toLong()))
        }
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
    "En hendelse av typen ${this.endringstype.name} har oppst√•tt for en ferdigstilt oppgave"

private fun Personhendelse.finnesIngenHendelseBeskrivelse() =
    "Det har oppst√•tt en personhendelse som det ikke finnes noen tidligere hendelse eller oppgave for. " +
        "Personhendelse id: ${this.hendelseId}, endringstype: ${this.endringstype.name}, opplysningstype ${this.opplysningstype}"

private fun Oppgave.er√Öpen() = !listOf(StatusEnum.FERDIGSTILT, StatusEnum.FEILREGISTRERT).contains(this.status)

private fun Oppgave.opph√łrtEllerAnnullertBeskrivelse() = "\n\nDenne oppgaven har opph√łrt eller blitt annullert."

private fun Oppgave.korrigertBeskrivelse() = "\n\nDenne oppgaven har blitt korrigert."

private fun Personhendelse.skalOpph√łreEllerKorrigeres() =
    listOf(Endringstype.ANNULLERT, Endringstype.KORRIGERT, Endringstype.OPPHOERT).contains(this.endringstype)
