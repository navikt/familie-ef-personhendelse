package no.nav.familie.ef.personhendelse.handler

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.utflytting.UtflyttingFraNorge
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtflyttingHandlerTest {

    private val sakClient = mockk<SakClient>()
    private val oppgaveClient = mockk<OppgaveClient>()


    private val utflyttingHandler = UtflyttingHandler(sakClient, oppgaveClient)

    private val personIdent = "12345612344"

    @Test
    fun `Ikke opprett oppgave for utflyttingshendelse dersom person ikke har løpende ef-sak`() {
        val personhendelse = Personhendelse()

        personhendelse.utflyttingFraNorge = UtflyttingFraNorge("Finland", "Helsinki", LocalDate.now())
        personhendelse.personidenter = listOf(personIdent)
        personhendelse.endringstype = Endringstype.OPPRETTET

        every { sakClient.finnesBehandlingForPerson(personIdent, StønadType.OVERGANGSSTØNAD) } returns false

        val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) } returns 123L

        utflyttingHandler.handleUtflytting(personhendelse)
        assertThat(oppgaveRequestSlot.isCaptured).isFalse
    }

    @Test
    fun `Opprett oppgave for sivilstand hendelse registrert partner dersom person har løpende ef-sak`() {
        val personhendelse = Personhendelse()
        personhendelse.utflyttingFraNorge = UtflyttingFraNorge("Finland", "Helsinki", LocalDate.now())
        personhendelse.personidenter = listOf(personIdent)
        personhendelse.endringstype = Endringstype.OPPRETTET

        every { sakClient.finnesBehandlingForPerson(personIdent, StønadType.OVERGANGSSTØNAD) } returns true

        val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) } returns 123L

        utflyttingHandler.handleUtflytting(personhendelse)

        assertThat(oppgaveRequestSlot.captured.oppgavetype).isEqualTo(Oppgavetype.VurderLivshendelse)
        assertThat(oppgaveRequestSlot.captured.beskrivelse).contains("Finland")
        assertThat(oppgaveRequestSlot.captured.ident?.ident).isEqualTo(personIdent)
    }
}