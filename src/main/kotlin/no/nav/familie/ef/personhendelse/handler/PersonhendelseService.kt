package no.nav.familie.ef.personhendelse.handler

import jakarta.transaction.Transactional
import no.nav.familie.ef.personhendelse.Hendelse
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.opprettVurderLivshendelseoppgave
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.familie.ef.personhendelse.util.identerUtenAktørId
import no.nav.familie.ef.personhendelse.utsattoppgave.UtsattOppgaveService
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
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
    private val personhendelseRepository: PersonhendelseRepository,
    private val utsattOppgaveService: UtsattOppgaveService,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    private val handlers: Map<String, PersonhendelseHandler> =
        personhendelseHandlers.associateBy { it.type.hendelsetype }

    init {
        logger.info("Legger til handlers: {}", personhendelseHandlers)
        if (personhendelseHandlers.isEmpty()) {
            error("Finner ikke handlers for personhendelse")
        }
    }

    fun håndterPersonhendelse(personhendelse: Personhendelse) {
        handlers[personhendelse.opplysningstype]?.let { handler ->
            handler.personidenterPerPersonSomSkalKontrolleres(personhendelse).forEach { personidenter ->
                if (personidenter.isEmpty()) {
                    error("Savner personidenter til personhendelse=${personhendelse.hendelseId}")
                }
                handle(handler, personhendelse, personidenter)
            }
        }
    }

    fun harHåndtertHendelse(hendelseId: String): Boolean = personhendelseRepository.hentHendelse(UUID.fromString(hendelseId)) != null

    private fun handle(
        handler: PersonhendelseHandler,
        personhendelse: Personhendelse,
        personidenter: Set<String>,
    ) {
        val harLøpendeStønad = sakClient.harLøpendeStønad(personidenter)
        if (harLøpendeStønad) {
            logger.info(
                "Håndterer hendelse: opplysningstype=${personhendelse.opplysningstype} av type=${personhendelse.endringstype.name}",
            )
            handlePersonhendelse(handler, personhendelse, personidenter.first())
        }
    }

    private fun handlePersonhendelse(
        handler: PersonhendelseHandler,
        personhendelse: Personhendelse,
        personIdent: String,
    ) {
        if (personhendelse.skalOpphøreEllerKorrigeres() && personhendelse.erIkkeOpphørAvSivilstand()) {
            opphørEllerKorrigerOppgave(personhendelse, handler.type)
            return
        }
        val oppgaveInformasjon = handler.lagOppgaveInformasjon(personhendelse)
        logHendelse(personhendelse, oppgaveInformasjon, personIdent)
        when (oppgaveInformasjon) {
            is IkkeOpprettOppgave -> {
                return
            }

            is OpprettOppgave -> {
                opprettOppgave(
                    UUID.fromString(personhendelse.hendelseId),
                    personhendelse.endringstype,
                    oppgaveInformasjon.beskrivelse,
                    personIdent,
                )
            }

            is UtsettOppgave -> {
                utsattOppgaveService.lagreUtsattOppgave(
                    personhendelse,
                    handlers[personhendelse.opplysningstype]?.type ?: error("Kunne ikke finne personopplysningstype"),
                    personIdent,
                    oppgaveInformasjon.beskrivelse,
                )
            }
        }
    }

    @Transactional
    fun opprettOppgaverAvUkesgamleHendelser() {
        val utsatteOppgaver = utsattOppgaveService.hentIkkeOpprettedeUtsatteOppgaverEldreEnnEnUke()
        utsatteOppgaver.forEach { utsattOppgave ->
            opprettOppgave(
                utsattOppgave.hendelsesId,
                utsattOppgave.endringstype,
                utsattOppgave.beskrivelse,
                utsattOppgave.personId,
            )
        }
        utsattOppgaveService.settUtsatteOppgaverTilUtført(utsatteOppgaver)
    }

    private fun logHendelse(
        personhendelse: Personhendelse,
        oppgaveBeskrivelse: OppgaveInformasjon,
        personIdent: String?,
    ) {
        val logMessage =
            "Finnes sak for opplysningstype=${personhendelse.opplysningstype}" +
                " hendelseId=${personhendelse.hendelseId}" +
                " endringstype=${personhendelse.endringstype}" +
                " skalOppretteOppgave=${oppgaveBeskrivelse is OpprettOppgave}"
        logger.info(logMessage)
        secureLogger.info("$logMessage personIdent=$personIdent")
    }

    private fun opprettOppgave(
        hendelseId: UUID,
        endringstype: Endringstype,
        beskrivelse: String,
        personIdent: String,
    ) {
        val opprettOppgaveRequest =
            opprettVurderLivshendelseoppgave(
                personIdent = personIdent,
                beskrivelse = "Personhendelse: $beskrivelse",
            )
        val oppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest)

        oppgaveClient.leggOppgaveIMappe(oppgaveId)

        lagreHendelse(hendelseId, oppgaveId, endringstype)
        logger.info("Oppgave opprettet med oppgaveId=$oppgaveId")
    }

    private fun opphørEllerKorrigerOppgave(
        personhendelse: Personhendelse,
        personhendelseType: PersonhendelseType,
    ) {
        val hendelse = hentTidligereHendelse(personhendelse)
        if (hendelse == null) {
            logger.warn(
                "Tidligere hendelse for personhendelse : ${personhendelse.hendelseId} ble ikke funnet. " +
                    personhendelse.finnesIngenHendelseBeskrivelse(),
            )
            return
        }
        val oppgave = hentOppgave(hendelse)
        if (oppgave.erÅpen()) {
            val nyOppgave =
                when (personhendelse.endringstype) {
                    Endringstype.ANNULLERT -> {
                        oppdater(
                            oppgave,
                            opphørtEllerAnnullertBeskrivelse(),
                            StatusEnum.FEILREGISTRERT,
                        )
                    }

                    Endringstype.OPPHOERT -> {
                        oppdater(
                            oppgave,
                            opphørtEllerAnnullertBeskrivelse(),
                            StatusEnum.FERDIGSTILT,
                        )
                    }

                    Endringstype.KORRIGERT -> {
                        oppdater(oppgave, korrigertBeskrivelse(), oppgave.status)
                    }

                    else -> {
                        error("Feil endringstype ved annullering eller korrigering : ${personhendelse.endringstype}")
                    }
                }
            logger.info("Oppgave oppdatert med oppgaveId=$nyOppgave for endringstype : ${personhendelse.endringstype.name}")
        } else {
            val ident = identFraOppgaveEllerPersonhendelse(oppgave, personhendelse)
            val nyOppgaveId = opprettOppgaveMedBeskrivelse(ident, personhendelse.ferdigstiltBeskrivelse(personhendelseType))
            logger.info("Ny oppgave=$nyOppgaveId ifm en allerede lukket oppgave er opprettet med oppgaveId=${oppgave.id}")
            oppgaveClient.leggOppgaveIMappe(nyOppgaveId)
        }
        lagreHendelse(
            UUID.fromString(personhendelse.hendelseId),
            oppgave.id ?: error("Kunne ikke finne oppgaveId for personhendelse: ${personhendelse.hendelseId}"),
            personhendelse.endringstype,
        )
    }

    private fun oppdater(
        oppgave: Oppgave,
        beskrivelse: String,
        status: StatusEnum?,
    ): Long {
        val nyOppgave = oppgave.copy(beskrivelse = oppgave.beskrivelse.plus(beskrivelse), status = status)
        return oppgaveClient.oppdaterOppgave(nyOppgave)
    }

    /**
     * Gjenbruker ident til forrige opprettede oppgave.
     * Dette da hendelsen kan være koblet til barnet og oppgaven som ble opprettet var opprettet på forelderen
     */
    private fun identFraOppgaveEllerPersonhendelse(
        oppgave: Oppgave,
        personhendelse: Personhendelse,
    ): String =
        oppgave.identer?.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
            ?: personhendelse.identerUtenAktørId().firstOrNull()
            ?: error("Finner ikke ident for personHendelse=${personhendelse.hendelseId}")

    private fun hentTidligereHendelse(personhendelse: Personhendelse): Hendelse? = personhendelseRepository.hentHendelse(UUID.fromString(personhendelse.tidligereHendelseId))

    private fun lagreHendelse(
        hendelseId: UUID,
        oppgaveId: Long,
        endringstype: Endringstype,
    ) {
        personhendelseRepository.lagrePersonhendelse(
            hendelseId,
            oppgaveId,
            endringstype,
        )
    }

    private fun hentOppgave(hendelse: Hendelse): Oppgave = oppgaveClient.finnOppgaveMedId(hendelse.oppgaveId)

    private fun opprettOppgaveMedBeskrivelse(
        personIdent: String,
        beskrivelse: String,
    ): Long =
        oppgaveClient.opprettOppgave(
            opprettVurderLivshendelseoppgave(
                personIdent = personIdent,
                beskrivelse = beskrivelse,
            ),
        )
}

private fun Personhendelse.ferdigstiltBeskrivelse(personhendelseType: PersonhendelseType) = "En hendelse av typen ${this.endringstype.name} har oppstått for en ferdigstilt oppgave med hendelsestype ${personhendelseType.name}"

private fun Personhendelse.finnesIngenHendelseBeskrivelse() =
    "Det har oppstått en personhendelse som det ikke finnes noen tidligere hendelse eller oppgave for. " +
        "Personhendelse id: ${this.hendelseId}, endringstype: ${this.endringstype.name}, opplysningstype ${this.opplysningstype}"

private fun Oppgave.erÅpen() = !listOf(StatusEnum.FERDIGSTILT, StatusEnum.FEILREGISTRERT).contains(this.status)

private fun opphørtEllerAnnullertBeskrivelse() = "\n\nDenne oppgaven har opphørt eller blitt annullert."

private fun korrigertBeskrivelse() = "\n\nDenne oppgaven har blitt korrigert."

private fun Personhendelse.skalOpphøreEllerKorrigeres() = listOf(Endringstype.ANNULLERT, Endringstype.KORRIGERT, Endringstype.OPPHOERT).contains(this.endringstype)

private fun Personhendelse.erIkkeOpphørAvSivilstand() = !(this.opplysningstype == PersonhendelseType.SIVILSTAND.hendelsetype && this.endringstype == Endringstype.OPPHOERT)
