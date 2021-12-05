package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.util.identerUtenAktørId
import no.nav.person.pdl.leesah.Personhendelse

interface PersonhendelseHandler {

    val type: PersonhendelseType

    fun skalOppretteOppgave(personhendelse: Personhendelse) = true

    fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): String

    /**
     * Returnerer en liste med personidenter for hver person som vi skal kontrollere
     * Eks når vi får en sivilstandshendelse returneres [["a1", "a2"]]
     * Mens når vi skal håndtere dødsfall returneres identer til personen og eventuellt forelder, [[f1], [f2], [b1]]
     */
    fun personidenterPerPersonSomSkalKontrolleres(personhendelse: Personhendelse): List<Set<String>> =
        listOf(personhendelse.identerUtenAktørId().toSet())
}
