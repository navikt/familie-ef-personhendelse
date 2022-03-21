package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.util.identerUtenAktørId
import no.nav.person.pdl.leesah.Personhendelse

data class OppgaveBeskrivelse(val skalOpprettes: Boolean = false, val beskrivelse: String? = null)

interface PersonhendelseHandler {

    val type: PersonhendelseType

    fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): OppgaveBeskrivelse

    /**
     * Returnerer en liste med personidenter for hver person som vi skal kontrollere
     * Eks når vi får en sivilstandshendelse returneres [["a1", "a2"]]
     * Mens når vi skal håndtere dødsfall returneres identer til personen og eventuellt forelder, [[f1], [f2], [b1]]
     */
    fun personidenterPerPersonSomSkalKontrolleres(personhendelse: Personhendelse): List<Set<String>> =
        listOf(personhendelse.identerUtenAktørId().toSet())
}
