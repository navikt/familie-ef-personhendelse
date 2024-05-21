package no.nav.familie.ef.personhendelse.dødsfalloppgaver

import no.nav.familie.ef.personhendelse.handler.PersonhendelseType
import no.nav.person.pdl.leesah.Personhendelse
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class DødsfallOppgaveService(
    private val dødsfallOppgaveRepository: DødsfallOppgaveRepository,
) {
    fun lagreDødsfallOppgave(
        personhendelse: Personhendelse,
        personhendelseType: PersonhendelseType,
        personIdent: String,
        beskrivelse: String,
    ) {
        dødsfallOppgaveRepository.lagreOppgave(
            UUID.fromString(personhendelse.hendelseId),
            personhendelseType,
            personhendelse.endringstype.name,
            personIdent,
            beskrivelse,
            LocalDateTime.now(),
        )
    }

    fun hentIkkeOpprettedeDødsfalloppgaverOverEnUkeTilbakeITid(): List<DødsfallOppgave> {
        return dødsfallOppgaveRepository.hentIkkeOpprettedeDødsfalloppgaverOverEnUkeTilbakeITid()
    }

    fun settDødsfalloppgaverTilUtført(oppgaver: List<DødsfallOppgave>) {
        oppgaver.forEach { dødsfallOppgaveRepository.settOppgaveTilUtført(it.hendelsesId) }
    }
}
