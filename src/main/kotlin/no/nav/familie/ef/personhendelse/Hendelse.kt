package no.nav.familie.ef.personhendelse

import no.nav.person.pdl.leesah.Endringstype
import java.time.LocalTime
import java.util.UUID

data class Hendelse(val hendelsesId : UUID, val oppgaveId : Long, val endringstype : Endringstype, val timestamp : LocalTime)