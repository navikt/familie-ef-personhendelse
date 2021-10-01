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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UtflyttingHandler(
    val sakClient: SakClient,
    val oppgaveClient: OppgaveClient,
    val personhendelseRepository: PersonhendelseRepository
) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun handleUtflytting(personhendelse: Personhendelse) {

        if (personhendelse.erAnnulleringEllerKorreksjon()) {
            handleAnnulleringEllerKorreksjon(personhendelse)
            return
        }

        personhendelse.utflyttingFraNorge?.let {
            logger.info("Mottatt utflyttingshendelse")
        } ?: throw Exception("Ingen utflyttingFraNorge tilordning i personhendelse : ${personhendelse}")

        val personIdent = personhendelse.personidenter.first()
        val finnesBehandlingForPerson = sakClient.finnesBehandlingForPerson(personIdent, StønadType.OVERGANGSSTØNAD)

        if (finnesBehandlingForPerson) {
            secureLogger.info("Behandling med personIdent finnes : $personIdent : $finnesBehandlingForPerson")
            val beskrivelse = personhendelse.utflyttingsBeskrivelse()
            val request = defaultOpprettOppgaveRequest(personIdent, beskrivelse)
            val oppgaveId = oppgaveClient.opprettOppgave(request)
            oppgaveId?.let {
                personhendelseRepository.lagrePersonhendelse(personhendelse.hendelseId, oppgaveId, personhendelse.endringstype)
            }
            secureLogger.info("Oppgave opprettet med oppgaveId: $oppgaveId")
        }

    }

    private fun handleAnnulleringEllerKorreksjon(personhendelse: Personhendelse) {
        val hendelse = personhendelseRepository.hentHendelse(UUID.fromString(personhendelse.hendelseId))?.let { it } ?: return
        val oppgave = oppgaveClient.finnOppgaveMedId(hendelse.oppgaveId)?.let { it } ?: return
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

private fun Personhendelse.utflyttingsBeskrivelse() =
    "Utflyttingshendelse til ${this.utflyttingFraNorge.tilflyttingsstedIUtlandet}, " +
            "{${this.utflyttingFraNorge.tilflyttingsland}. " +
            "Utflyttingsdato : ${this.utflyttingFraNorge.utflyttingsdato}"

private fun Personhendelse.erAnnulleringEllerKorreksjon() =
    listOf(Endringstype.ANNULLERT, Endringstype.KORRIGERT).contains(this.endringstype)

private fun Oppgave.erOppgaveÅpen() = !listOf(StatusEnum.FERDIGSTILT, StatusEnum.FEILREGISTRERT).contains(this.status)

private fun Oppgave.annullertEllerKorrigertBeskrivelse() = "\n\nDenne oppgaven har blitt annullert eller korrigert."
