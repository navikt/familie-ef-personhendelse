package no.nav.familie.ef.personhendelse.utsattoppgave

import no.nav.familie.ef.personhendelse.handler.PersonhendelseType
import no.nav.person.pdl.leesah.Endringstype
import java.time.LocalDateTime
import java.util.UUID

data class UtsattOppgave(
    val hendelsesId: UUID,
    val personId: String,
    val beskrivelse: String,
    val personhendelseType: PersonhendelseType,
    val endringstype: Endringstype,
    val hendelsesTid: LocalDateTime,
    val opprettetOppgaveTid: LocalDateTime?,
)
