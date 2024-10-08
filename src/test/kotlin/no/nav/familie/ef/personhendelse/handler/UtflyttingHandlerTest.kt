package no.nav.familie.ef.personhendelse.handler

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.familie.ef.personhendelse.utsattoppgave.UtsattOppgaveService
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
    val sakClient = mockk<SakClient>()
    val oppgaveClient = mockk<OppgaveClient>(relaxed = true)
    val personhendelseRepository = mockk<PersonhendelseRepository>()
    private val utsattOppgaveService = mockk<UtsattOppgaveService>()

    private val handler = UtflyttingHandler()
    private val service = PersonhendelseService(listOf(handler), sakClient, oppgaveClient, personhendelseRepository, utsattOppgaveService)

    private val personIdent = "12345612344"

    @Test
    fun `Ikke opprett oppgave for utflyttingshendelse dersom person ikke har løpende ef-sak`() {
        val personhendelse = Personhendelse()
        personhendelse.opplysningstype = PersonhendelseType.UTFLYTTING_FRA_NORGE.hendelsetype
        personhendelse.utflyttingFraNorge = UtflyttingFraNorge("Finland", "Helsinki", LocalDate.now())
        personhendelse.personidenter = listOf(personIdent)
        personhendelse.endringstype = Endringstype.OPPRETTET
        personhendelse.hendelseId = UUID.randomUUID().toString()

        every { sakClient.harLøpendeStønad(setOf(personIdent)) } returns false
        every { personhendelseRepository.lagrePersonhendelse(any(), any(), any()) } just runs

        val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) } returns 123L

        service.håndterPersonhendelse(personhendelse)
        assertThat(oppgaveRequestSlot.isCaptured).isFalse
    }

    @Test
    fun `Opprett oppgave for sivilstand hendelse registrert partner dersom person har løpende ef-sak`() {
        val personhendelse = Personhendelse()
        personhendelse.opplysningstype = PersonhendelseType.UTFLYTTING_FRA_NORGE.hendelsetype
        personhendelse.utflyttingFraNorge = UtflyttingFraNorge("Finland", "Helsinki", LocalDate.now())
        personhendelse.personidenter = listOf(personIdent)
        personhendelse.endringstype = Endringstype.OPPRETTET
        personhendelse.hendelseId = UUID.randomUUID().toString()

        every { sakClient.harLøpendeStønad(setOf(personIdent)) } returns true
        every { personhendelseRepository.lagrePersonhendelse(any(), any(), any()) } just runs

        val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) } returns 123L

        service.håndterPersonhendelse(personhendelse)

        assertThat(oppgaveRequestSlot.captured.oppgavetype).isEqualTo(Oppgavetype.VurderLivshendelse)
        assertThat(oppgaveRequestSlot.captured.beskrivelse).contains("Finland")
        assertThat(oppgaveRequestSlot.captured.ident?.ident).isEqualTo(personIdent)
    }
}
