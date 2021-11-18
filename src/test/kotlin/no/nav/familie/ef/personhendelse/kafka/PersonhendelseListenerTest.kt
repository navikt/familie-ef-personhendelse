package no.nav.familie.ef.personhendelse.kafka

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.handler.PersonhendelseService
import no.nav.familie.ef.personhendelse.handler.PersonhendelseType
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersonhendelseListenerTest {

    private val sakClient = mockk<SakClient>()
    private val personhendelseService = mockk<PersonhendelseService>(relaxed = true)

    private lateinit var listener: PersonhendelseListener

    private val personMedSak = "11111111111"
    private val personUtenSak = "22222222222"

    @BeforeEach
    internal fun setUp() {
        listener = PersonhendelseListener("", personhendelseService)
        every { sakClient.harStønadSiste12MånederForPersonidenter(setOf(personMedSak)) } returns true
        every { sakClient.harStønadSiste12MånederForPersonidenter(setOf(personUtenSak)) } returns false
    }

    @Test
    internal fun `skal kalle på personhendelseService for hendelse`() {
        listener.listen(lagPersonhendelse(personIdent = personUtenSak))

        verify(exactly = 1) { personhendelseService.håndterPersonhendelse(any()) }
    }

    @Test
    internal fun `skal kaste feil når hendelse mangler personidenter`() {
        val personhendelse = lagPersonhendelse(personIdent = "")
        assertThatThrownBy { listener.listen(personhendelse) }.hasMessageContaining("Hendelse uten personIdent")

        verify(exactly = 0) { personhendelseService.håndterPersonhendelse(any()) }
    }

    private fun lagPersonhendelse(
            endringstype: Endringstype = Endringstype.OPPRETTET,
            personIdent: String = this.personMedSak,
            opplysningstype: PersonhendelseType = PersonhendelseType.SIVILSTAND
    ): Personhendelse {
        val personhendelse = Personhendelse()
        personhendelse.personidenter = listOf(personIdent)
        personhendelse.endringstype = endringstype
        personhendelse.opplysningstype = opplysningstype.hendelsetype
        return personhendelse
    }

}
