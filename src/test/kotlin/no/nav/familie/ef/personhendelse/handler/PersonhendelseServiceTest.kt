package no.nav.familie.ef.personhendelse.handler

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ef.personhendelse.Hendelse
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
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

    @BeforeEach
    internal fun setUp() {
        every { oppgaveClient.finnOppgaveMedId(any()) }.returns(Oppgave(id = 0L, status = StatusEnum.OPPRETTET))
        every { oppgaveClient.oppdaterOppgave(any()) }.returns(0L)
        every { oppgaveClient.opprettOppgave(any()) }.returns(0L)
        every { sakClient.finnesBehandlingForPerson(any()) }.returns(true)
        every { personhendelseRepository.lagrePersonhendelse(any(), any(), any()) } just runs
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
        val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) }.returns(0L)
        personhendelseService.håndterPersonhendelse(personhendelse)
        assertThat(oppgaveRequestSlot.captured.beskrivelse).contains(dummyHandler.lagOppgaveBeskrivelse(personhendelse))
    }

    @Test
    fun `finnes ingen behandling for person, forvent at hendelsen ikke håndteres`() {
        val personhendelse = personhendelse(Endringstype.OPPRETTET)
        every { sakClient.finnesBehandlingForPerson(any()) }.returns(false)
        val handlerSpyk = spyk(personhendelseService, recordPrivateCalls = true)
        handlerSpyk.håndterPersonhendelse(personhendelse)
        verify(exactly = 0) {
            handlerSpyk["handlePersonhendelse"](any<PersonhendelseHandler>(),
                                                any<Personhendelse>(),
                                                any<String>())
        }
    }

    @Test
    fun `handler har skalOppretteOppgave lik false, forvent at oppgave ikke opprettes`() {
        val personhendelse = personhendelse(Endringstype.OPPRETTET)
        dummyHandler.skalOppretteOppgave = false
        val personhendelseService =
                PersonhendelseService(listOf(dummyHandler), sakClient, oppgaveClient, personhendelseRepository)
        personhendelseService.håndterPersonhendelse(personhendelse)
        verify(exactly = 0) { oppgaveClient.opprettOppgave(any()) }
    }

    @Test
    fun `håndter personhendelse av typen annuller uten en tidligere hendelse, forvent oppgaveopprettelse`() {
        val personhendelse = personhendelse(Endringstype.ANNULLERT)
        every { personhendelseRepository.hentHendelse(any()) }.returns(null)
        personhendelseService.håndterPersonhendelse(personhendelse)
        verify { oppgaveClient.opprettOppgave(any()) }
    }

    @Test
    fun `send personhendelse av typen annuller til en tilknyttet oppgave, forvent oppgaveoppdatering`() {
        val personhendelse = personhendelse(Endringstype.ANNULLERT)
        val dummyHendelse = Hendelse(UUID.randomUUID(), 0L, Endringstype.ANNULLERT, LocalDateTime.now())
        every { personhendelseRepository.hentHendelse(any()) }.returns(dummyHendelse)
        personhendelseService.håndterPersonhendelse(personhendelse)
        verify { oppgaveClient.oppdaterOppgave(any()) }
    }

    @Test
    fun `send personhendelse av typen annuller til en tilknyttet og ferdigstilt oppgave, forvent oppgaveopprettelse`() {
        val personhendelse = personhendelse(Endringstype.ANNULLERT)

        val dummyHendelse = Hendelse(UUID.randomUUID(), 0L, Endringstype.ANNULLERT, LocalDateTime.now())
        every { personhendelseRepository.hentHendelse(any()) }.returns(dummyHendelse)
        every { oppgaveClient.finnOppgaveMedId(any()) }.returns(Oppgave(id = 0L, status = StatusEnum.FERDIGSTILT))
        personhendelseService.håndterPersonhendelse(personhendelse)
        verify { oppgaveClient.opprettOppgave(any()) }
    }

    private fun personhendelse(endringstype: Endringstype): Personhendelse {
        var personhendelse = Personhendelse()
        personhendelse.personidenter = mutableListOf("12345612344")
        personhendelse.endringstype = endringstype
        personhendelse.hendelseId = UUID.randomUUID().toString()
        personhendelse.tidligereHendelseId = UUID.randomUUID().toString()
        personhendelse.opplysningstype = PersonhendelseType.UTFLYTTING_FRA_NORGE.name
        return personhendelse
    }

    class DummyHandler : PersonhendelseHandler {

        override val type = PersonhendelseType.UTFLYTTING_FRA_NORGE
        var skalOppretteOppgave = true

        override fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): String {
            return "Dummyhandler"
        }

        override fun skalOppretteOppgave(personhendelse: Personhendelse): Boolean {
            return skalOppretteOppgave
        }
    }
}