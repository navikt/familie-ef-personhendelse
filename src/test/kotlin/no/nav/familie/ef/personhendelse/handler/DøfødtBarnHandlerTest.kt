package no.nav.familie.ef.personhendelse.handler

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.doedfoedtbarn.DoedfoedtBarn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class DøfødtBarnHandlerTest {

    private val sakClient = mockk<SakClient>()
    private val oppgaveClient = mockk<OppgaveClient>()

    private val handler = DøfødtBarnHandler(sakClient, oppgaveClient)

    private val personIdent = "12345612344"

    private val slot = slot<OpprettOppgaveRequest>()

    @BeforeEach
    internal fun setUp() {
        every { sakClient.finnesBehandlingForPerson(any(), any()) } returns true
        every { oppgaveClient.opprettOppgave(capture(slot)) } returns 1L
    }

    @Test
    internal fun `skal behandle døfødt barn uten dato`() {
        val personhendelse = dødfødtBarn(null)

        handler.handle(personhendelse)

        verify(exactly = 1) { oppgaveClient.opprettOppgave(any()) }
        assertThat(slot.captured.beskrivelse).isEqualTo("Personhendelse: Døfødt barn ukjent dato")
    }

    @Test
    internal fun `skal behandle døfødt barn med dato`() {
        val personhendelse = dødfødtBarn(LocalDate.now())

        handler.handle(personhendelse)

        verify(exactly = 1) { oppgaveClient.opprettOppgave(any()) }
        assertThat(slot.captured.beskrivelse).isEqualTo("Personhendelse: Døfødt barn 01.10.2021")
    }

    private fun dødfødtBarn(dato: LocalDate?): Personhendelse {
        val personhendelse = Personhendelse()
        personhendelse.personidenter = listOf(personIdent)
        personhendelse.hendelseId = UUID.randomUUID().toString()
        personhendelse.doedfoedtBarn = DoedfoedtBarn(dato)
        return personhendelse
    }
}