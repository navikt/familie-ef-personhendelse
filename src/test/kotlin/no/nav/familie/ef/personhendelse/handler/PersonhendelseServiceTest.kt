package no.nav.familie.ef.personhendelse.handler

import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ef.personhendelse.Hendelse
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class PersonhendelseServiceTest {

    private val sakClient = mockk<SakClient>()
    private val oppgaveClient = mockk<OppgaveClient>()
    private val personhendelseRepository = mockk<PersonhendelseRepository>()
    private val dummyHandler: DummyHandler = DummyHandler()

    private val personhendelseService =
        PersonhendelseService(listOf(dummyHandler), sakClient, oppgaveClient, personhendelseRepository)

    private val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()

    private val personHendelseIdent = "11111111111"
    private val annenIdent = "2"
    private val oppgave = Oppgave(
        id = 0L,
        status = StatusEnum.OPPRETTET,
        identer = listOf(OppgaveIdentV2(annenIdent, IdentGruppe.FOLKEREGISTERIDENT)),
    )

    @BeforeEach
    internal fun setUp() {
        every { oppgaveClient.finnOppgaveMedId(any()) } returns oppgave
        every { oppgaveClient.oppdaterOppgave(any()) } returns 0L
        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) } returns 0L
        every { sakClient.harLøpendeStønad(any()) } returns true
        every { personhendelseRepository.lagrePersonhendelse(any(), any(), any()) } just runs
        justRun { oppgaveClient.leggOppgaveIMappe(any()) }
    }

    @Test
    fun `endringstype OPPRETTET håndteres og behandling finnes for person, forvent at oppgave opprettes`() {
        val personhendelse = personhendelse(Endringstype.OPPRETTET)

        personhendelseService.håndterPersonhendelse(personhendelse)

        verify { oppgaveClient.opprettOppgave(any()) }
    }

    @Test
    fun `endringstype OPPRETTET håndteres og behandling finnes for person, forvent at beskrivelse for handler sendes`() {
        val personhendelse = personhendelse(Endringstype.OPPRETTET)

        personhendelseService.håndterPersonhendelse(personhendelse)

        assertThat(oppgaveRequestSlot.captured.beskrivelse).contains(dummyHandler.lagOppgaveBeskrivelse(personhendelse).beskrivelse)
    }

    @Test
    fun `finnes ingen behandling for person, forvent at hendelsen ikke håndteres`() {
        val personhendelse = personhendelse(Endringstype.OPPRETTET)
        every { sakClient.harLøpendeStønad(any()) } returns false
        val handlerSpyk = spyk(personhendelseService, recordPrivateCalls = true)

        handlerSpyk.håndterPersonhendelse(personhendelse)

        verify(exactly = 0) {
            handlerSpyk["handlePersonhendelse"](
                any<PersonhendelseHandler>(),
                any<Personhendelse>(),
                any<String>(),
            )
        }
    }

    @Test
    fun `håndter personhendelse av typen annuller uten en tidligere hendelse, ikke opprett oppgave`() {
        val personhendelse = personhendelse(Endringstype.ANNULLERT)
        every { personhendelseRepository.hentHendelse(any()) } returns null

        personhendelseService.håndterPersonhendelse(personhendelse)

        verify(exactly = 0) { oppgaveClient.opprettOppgave(any()) }
    }

    @Test
    fun `send personhendelse av typen annuller til en tilknyttet oppgave, forvent oppgaveoppdatering`() {
        val personhendelse = personhendelse(Endringstype.ANNULLERT)
        val dummyHendelse = Hendelse(UUID.randomUUID(), 0L, Endringstype.ANNULLERT, LocalDateTime.now())
        every { personhendelseRepository.hentHendelse(any()) } returns dummyHendelse

        personhendelseService.håndterPersonhendelse(personhendelse)

        verify { oppgaveClient.oppdaterOppgave(any()) }
    }

    @Test
    fun `send personhendelse av typen annuller til en tilknyttet og ferdigstilt oppgave, forvent oppgaveopprettelse`() {
        val personhendelse = personhendelse(Endringstype.ANNULLERT)

        val dummyHendelse = Hendelse(UUID.randomUUID(), 0L, Endringstype.ANNULLERT, LocalDateTime.now())
        every { personhendelseRepository.hentHendelse(any()) } returns dummyHendelse
        every { oppgaveClient.finnOppgaveMedId(any()) } returns Oppgave(id = 0L, status = StatusEnum.FERDIGSTILT)

        personhendelseService.håndterPersonhendelse(personhendelse)

        verify { oppgaveClient.opprettOppgave(any()) }
    }

    @Test
    internal fun `korrigert hendelse skal opprette oppgave på forrige oppgave sin ident`() {
        val personhendelse = personhendelse(Endringstype.KORRIGERT)
        val hendelse = Hendelse(UUID.randomUUID(), 100L, Endringstype.OPPRETTET, LocalDateTime.now())

        every { oppgaveClient.finnOppgaveMedId(any()) } returns oppgave.copy(status = StatusEnum.FERDIGSTILT)
        every { personhendelseRepository.hentHendelse(any()) } returns hendelse

        personhendelseService.håndterPersonhendelse(personhendelse)

        assertThat(oppgaveRequestSlot.captured.ident?.ident).isEqualTo(annenIdent)
        verify(exactly = 1) { oppgaveClient.leggOppgaveIMappe(any()) }
    }

    private fun personhendelse(endringstype: Endringstype): Personhendelse = Personhendelse().apply {
        personidenter = listOf(personHendelseIdent)
        this.endringstype = endringstype
        hendelseId = UUID.randomUUID().toString()
        tidligereHendelseId = UUID.randomUUID().toString()
        opplysningstype = PersonhendelseType.UTFLYTTING_FRA_NORGE.name
    }

    class DummyHandler : PersonhendelseHandler {

        override val type = PersonhendelseType.UTFLYTTING_FRA_NORGE

        override fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): OpprettOppgave {
            return OpprettOppgave(beskrivelse = "Dummy handler")
        }
    }
}
