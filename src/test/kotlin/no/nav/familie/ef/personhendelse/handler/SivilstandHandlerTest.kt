package no.nav.familie.ef.personhendelse.handler

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.personhendelse.Hendelse
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.datoutil.tilNorskDatoformat
import no.nav.familie.ef.personhendelse.dødsfalloppgaver.DødsfallOppgaveService
import no.nav.familie.ef.personhendelse.generated.enums.Sivilstandstype
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.sivilstand.Sivilstand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SivilstandHandlerTest {
    private val sakClient = mockk<SakClient>()
    private val oppgaveClient = mockk<OppgaveClient>(relaxed = true)
    private val personhendelseRepository = mockk<PersonhendelseRepository>()
    private val dødsfallOppgaveService = mockk<DødsfallOppgaveService>()
    private val handler = SivilstandHandler()
    private val service = PersonhendelseService(listOf(handler), sakClient, oppgaveClient, personhendelseRepository, dødsfallOppgaveService)
    private val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()

    private val personIdent = "12345612344"
    private val partnerPersonIdent = "12345612345"

    @BeforeEach
    fun setup() {
        every { sakClient.harLøpendeStønad(any()) } returns true
        every { personhendelseRepository.lagrePersonhendelse(any(), any(), any()) } just runs
        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) } returns 123L
    }

    @Test
    fun `Ikke opprett oppgave for sivilstand hendelse registrert partner dersom person ikke har løpende ef-sak`() {
        val personhendelse = createSivilstandHendelse(Sivilstandstype.REGISTRERT_PARTNER, Endringstype.OPPRETTET)

        every { sakClient.harLøpendeStønad(setOf(personIdent)) } returns false

        service.håndterPersonhendelse(personhendelse)

        assertThat(oppgaveRequestSlot.isCaptured).isFalse
    }

    @Test
    fun `Opprett oppgave for sivilstand hendelse registrert partner dersom person har løpende ef-sak`() {
        val personhendelse = createSivilstandHendelse(Sivilstandstype.REGISTRERT_PARTNER, Endringstype.OPPRETTET)

        service.håndterPersonhendelse(personhendelse)

        assertThat(oppgaveRequestSlot.captured.oppgavetype).isEqualTo(Oppgavetype.VurderLivshendelse)
        val iDagNorskDatoformat = LocalDate.now().tilNorskDatoformat()
        assertThat(oppgaveRequestSlot.captured.beskrivelse)
            .isEqualTo("Personhendelse: Ny sivilstand av type \"Registrert partner\", gyldig fra og med dato: $iDagNorskDatoformat")
        assertThat(oppgaveRequestSlot.captured.ident?.ident).isEqualTo(personIdent)
    }

    @Test
    fun `Ikke opprett oppgave dersom bruker blir skilt`() {
        val personhendelse = createSivilstandHendelse(Sivilstandstype.SKILT, Endringstype.OPPRETTET)
        service.håndterPersonhendelse(personhendelse)

        assertThat(oppgaveRequestSlot.isCaptured).isFalse
    }

    @Test
    fun `Ikke opprett oppgave dersom bruker blir separert`() {
        val personhendelse = createSivilstandHendelse(Sivilstandstype.SEPARERT, Endringstype.OPPRETTET)
        service.håndterPersonhendelse(personhendelse)

        assertThat(oppgaveRequestSlot.isCaptured).isFalse
    }

    @Test
    fun `Korrigering av separasjon-hendelse`() {
        every {
            personhendelseRepository.hentHendelse(
                any(),
            )
        } returns Hendelse(UUID.randomUUID(), 1, Endringstype.OPPRETTET, LocalDateTime.now())
        val personhendelse = createSivilstandHendelse(Sivilstandstype.SEPARERT, Endringstype.KORRIGERT)
        personhendelse.tidligereHendelseId = UUID.randomUUID().toString()
        service.håndterPersonhendelse(personhendelse)

        assertThat(oppgaveRequestSlot.isCaptured).isFalse
        verify(exactly = 1) { oppgaveClient.oppdaterOppgave(any()) }
    }

    @Test
    fun `Opprett oppgave der brukers separasjon opphører`() {
        every {
            personhendelseRepository.hentHendelse(
                any(),
            )
        } returns Hendelse(UUID.randomUUID(), 1, Endringstype.OPPRETTET, LocalDateTime.now())

        val personhendelse = createSivilstandHendelse(Sivilstandstype.SEPARERT, Endringstype.OPPHOERT)
        service.håndterPersonhendelse(personhendelse)

        assertThat(oppgaveRequestSlot.captured.oppgavetype).isEqualTo(Oppgavetype.VurderLivshendelse)
        val iDagNorskDatoformat = LocalDate.now().tilNorskDatoformat()
        assertThat(oppgaveRequestSlot.captured.beskrivelse)
            .isEqualTo("Personhendelse: Opphør av sivilstand av type \"Separert\", gyldig fra og med dato: $iDagNorskDatoformat")
        assertThat(oppgaveRequestSlot.captured.ident?.ident).isEqualTo(personIdent)
    }

    private fun createSivilstandHendelse(
        sivilstandstype: Sivilstandstype,
        endringstype: Endringstype,
    ): Personhendelse {
        val personhendelse = Personhendelse()
        personhendelse.sivilstand =
            Sivilstand(
                sivilstandstype.name,
                LocalDate.now(),
                partnerPersonIdent,
                LocalDate.now(),
            )
        personhendelse.opplysningstype = PersonhendelseType.SIVILSTAND.hendelsetype
        personhendelse.personidenter = listOf(personIdent)
        personhendelse.endringstype = endringstype
        personhendelse.hendelseId = UUID.randomUUID().toString()
        if (endringstype == Endringstype.OPPHOERT) {
            personhendelse.tidligereHendelseId = UUID.randomUUID().toString()
        }

        return personhendelse
    }
}
