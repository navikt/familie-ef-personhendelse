package no.nav.familie.ef.personhendelse.util

import no.nav.person.pdl.leesah.Personhendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PersonidenterUtilKtTest {
    private val fnr = "11111154321"
    private val aktørId = "1245678901245"

    @Test
    internal fun `skal filtrere vekk aktørId fra personidenter`() {
        val personhendelse = Personhendelse()
        personhendelse.personidenter = listOf(aktørId, fnr, aktørId)
        val personidenter = personhendelse.identerUtenAktørId()
        assertThat(personidenter).isEqualTo(setOf(fnr))
    }
}
