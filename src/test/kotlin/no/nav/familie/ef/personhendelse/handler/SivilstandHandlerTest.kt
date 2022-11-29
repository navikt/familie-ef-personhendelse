package no.nav.familie.ef.personhendelse.handler

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.datoutil.tilNorskDatoformat
import no.nav.familie.ef.personhendelse.generated.enums.Sivilstandstype
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.sivilstand.Sivilstand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class SivilstandHandlerTest {

    private val sakClient = mockk<SakClient>()
    private val oppgaveClient = mockk<OppgaveClient>(relaxed = true)
    private val personhendelseRepository = mockk<PersonhendelseRepository>()
    private val handler = SivilstandHandler()
    private val service = PersonhendelseService(listOf(handler), sakClient, oppgaveClient, personhendelseRepository)

    private val personIdent = "12345612344"
    private val partnerPersonIdent = "12345612345"

    @Test
    fun `Ikke opprett oppgave for sivilstand hendelse registrert partner dersom person ikke har løpende ef-sak`() {
        val personhendelse = Personhendelse()
        personhendelse.sivilstand = Sivilstand(
            Sivilstandstype.REGISTRERT_PARTNER.name,
            LocalDate.now(),
            partnerPersonIdent,
            LocalDate.now()
        )
        personhendelse.opplysningstype = PersonhendelseType.SIVILSTAND.hendelsetype
        personhendelse.personidenter = listOf(personIdent)
        personhendelse.endringstype = Endringstype.OPPRETTET

        every { sakClient.harLøpendeStønad(setOf(personIdent)) } returns false

        val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) } returns 123L

        service.håndterPersonhendelse(personhendelse)

        assertThat(oppgaveRequestSlot.isCaptured).isFalse
    }

    @Test
    fun `Ikke opprett oppgave for sivilstand hendelse registrert partner dersom datoet for hendelsen er eldre enn 2 år`() {
        val personhendelse = Personhendelse()
        personhendelse.sivilstand = Sivilstand(
            Sivilstandstype.REGISTRERT_PARTNER.name,
            LocalDate.now().minusYears(2).minusMonths(1),
            partnerPersonIdent,
            LocalDate.now().minusYears(2).minusMonths(1)
        )
        personhendelse.opplysningstype = PersonhendelseType.SIVILSTAND.hendelsetype
        personhendelse.personidenter = listOf(personIdent)
        personhendelse.endringstype = Endringstype.OPPRETTET

        every { sakClient.harLøpendeStønad(setOf(personIdent)) } returns true

        val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) } returns 123L

        service.håndterPersonhendelse(personhendelse)

        assertThat(oppgaveRequestSlot.isCaptured).isFalse
    }

    @Test
    fun `Opprett oppgave for sivilstand hendelse registrert partner dersom person har løpende ef-sak`() {
        val personhendelse = Personhendelse()
        personhendelse.sivilstand = Sivilstand(
            Sivilstandstype.REGISTRERT_PARTNER.name,
            LocalDate.now(),
            partnerPersonIdent,
            LocalDate.now()
        )
        personhendelse.opplysningstype = PersonhendelseType.SIVILSTAND.hendelsetype
        personhendelse.personidenter = listOf(personIdent)
        personhendelse.endringstype = Endringstype.OPPRETTET
        personhendelse.hendelseId = UUID.randomUUID().toString()

        every { sakClient.harLøpendeStønad(setOf(personIdent)) } returns true
        every { personhendelseRepository.lagrePersonhendelse(any(), any(), any()) } just runs
        every { personhendelseRepository.lagrePersonhendelse(any(), any(), any()) } just runs

        val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) } returns 123L

        service.håndterPersonhendelse(personhendelse)

        assertThat(oppgaveRequestSlot.captured.oppgavetype).isEqualTo(Oppgavetype.VurderLivshendelse)
        val iDagNorskDatoformat = LocalDate.now().tilNorskDatoformat()
        assertThat(oppgaveRequestSlot.captured.beskrivelse)
            .isEqualTo("Personhendelse: Sivilstand endret til \"Registrert partner\", gyldig fra og med dato: $iDagNorskDatoformat")
        assertThat(oppgaveRequestSlot.captured.ident?.ident).isEqualTo(personIdent)
    }
}
