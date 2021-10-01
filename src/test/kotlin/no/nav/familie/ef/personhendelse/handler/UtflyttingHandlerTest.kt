package no.nav.familie.ef.personhendelse.handler

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.utflytting.UtflyttingFraNorge
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class UtflyttingHandlerTest {

    val sakClient: SakClient = mockk<SakClient>()
    val oppgaveClient: OppgaveClient = mockk<OppgaveClient>()
    val personhendelseRepository: PersonhendelseRepository = mockk<PersonhendelseRepository>()

    private val utflyttingHandler = UtflyttingHandler(sakClient, oppgaveClient, personhendelseRepository)

    private val personIdent = "12345612344"

    @Test
    fun `Ikke opprett oppgave for utflyttingshendelse dersom person ikke har løpende ef-sak`() {
        val personhendelse = Personhendelse()

        personhendelse.utflyttingFraNorge = UtflyttingFraNorge("Finland", "Helsinki", LocalDate.now())
        personhendelse.personidenter = listOf(personIdent)
        personhendelse.endringstype = Endringstype.OPPRETTET
        personhendelse.hendelseId = UUID.randomUUID().toString()

        every { sakClient.finnesBehandlingForPerson(personIdent, StønadType.OVERGANGSSTØNAD) } returns false
        every { personhendelseRepository.lagrePersonhendelse(any(), any(), any()) } just runs

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
        personhendelse.hendelseId = UUID.randomUUID().toString()

        every { sakClient.finnesBehandlingForPerson(personIdent, StønadType.OVERGANGSSTØNAD) } returns true
        every { personhendelseRepository.lagrePersonhendelse(any(), any(), any()) } just runs

        val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) } returns 123L

        utflyttingHandler.handleUtflytting(personhendelse)

        assertThat(oppgaveRequestSlot.captured.oppgavetype).isEqualTo(Oppgavetype.VurderLivshendelse)
        assertThat(oppgaveRequestSlot.captured.beskrivelse).contains("Finland")
        assertThat(oppgaveRequestSlot.captured.ident?.ident).isEqualTo(personIdent)
    }
}