package no.nav.familie.ef.personhendelse.forsinketoppgave

import no.nav.familie.ef.personhendelse.handler.PersonhendelseType
import no.nav.person.pdl.leesah.Personhendelse
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class ForsinketOppgaveService(
    private val forsinketOppgaveRepository: ForsinketOppgaveRepository,
) {
    fun lagreForsinketOppgave(
        personhendelse: Personhendelse,
        personhendelseType: PersonhendelseType,
        personIdent: String,
        beskrivelse: String,
    ) {
        forsinketOppgaveRepository.lagreOppgave(
            UUID.fromString(personhendelse.hendelseId),
            personhendelseType,
            personhendelse.endringstype.name,
            personIdent,
            beskrivelse,
            LocalDateTime.now(),
        )
    }

    fun hentIkkeOpprettedeForsinkedeOppgaverOverEnUkeTilbakeITid(): List<ForsinketOppgave> = forsinketOppgaveRepository.hentIkkeOpprettedeForsinkedeOppgaverOverEnUkeTilbakeITid()

    fun settForsinkedeOppgaverTilUtført(oppgaver: List<ForsinketOppgave>) {
        oppgaver.forEach { forsinketOppgaveRepository.settOppgaveTilUtført(it.hendelsesId) }
    }
}
