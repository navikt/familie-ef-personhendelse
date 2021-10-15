package no.nav.familie.ef.personhendelse.kafka

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.handler.PersonhendelseHandler
import no.nav.familie.ef.personhendelse.handler.PersonhendelseType
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersonhendelseListenerTest {

    private val sakClient = mockk<SakClient>()
    private val oppgaveClient = mockk<OppgaveClient>()
    private val personhendelseRepository = mockk<PersonhendelseRepository>()

    private lateinit var handler: PersonhendelseHandler
    private lateinit var listener: PersonhendelseListener

    private val personMedSak = "11111111111"
    private val personUtenSak = "22222222222"

    @BeforeEach
    internal fun setUp() {
        handler = lagHandler()
        listener = PersonhendelseListener(listOf(handler), "")
        every { sakClient.finnesBehandlingForPerson(setOf(personMedSak)) } returns true
        every { sakClient.finnesBehandlingForPerson(setOf(personUtenSak)) } returns false
    }

    @Test
    internal fun `skal kalle på handler for hendelse som har handler`() {
        listener.listen(lagPersonhendelse(personIdent = personUtenSak))

        verify(exactly = 1) { handler.handle(any()) }
    }

    @Test
    internal fun `skal ikke kalle på handler for hendelse som ikke er mappet med handler`() {
        val personhendelse = lagPersonhendelse()
        personhendelse.opplysningstype = "IKKE_MAPPET"

        listener.listen(personhendelse)

        verify(exactly = 0) { handler.handle(any()) }
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

    private fun lagHandler() =
        spyk<PersonhendelseHandler>(object : PersonhendelseHandler(sakClient, oppgaveClient, personhendelseRepository) {
            override val type: PersonhendelseType = PersonhendelseType.SIVILSTAND

            override fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): String {
                return "beskrivelse"
            }

        })
}