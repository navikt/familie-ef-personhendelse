package no.nav.familie.ef.personhendelse.utsattoppgave

import no.nav.familie.ef.personhendelse.handler.PersonhendelseType
import no.nav.person.pdl.leesah.Personhendelse
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class UtsattOppgaveService(
    private val utsattOppgaveRepository: UtsattOppgaveRepository,
) {
    fun lagreUtsattOppgave(
        personhendelse: Personhendelse,
        personhendelseType: PersonhendelseType,
        personIdent: String,
        beskrivelse: String,
    ) {
        utsattOppgaveRepository.lagreOppgave(
            UUID.fromString(personhendelse.hendelseId),
            personhendelseType,
            personhendelse.endringstype.name,
            personIdent,
            beskrivelse,
            LocalDateTime.now(),
        )
    }

    fun hentIkkeOpprettedeUtsatteOppgaverEldreEnnEnUke(): List<UtsattOppgave> = utsattOppgaveRepository.hentIkkeOpprettedeUtsatteOppgaverEldreEnnEnUke()

    fun settUtsatteOppgaverTilUtført(oppgaver: List<UtsattOppgave>) {
        oppgaver.forEach { utsattOppgaveRepository.settOppgaveTilUtført(it.hendelsesId) }
    }
}
