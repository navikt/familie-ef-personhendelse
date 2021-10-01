package no.nav.familie.ef.personhendelse.handler

import io.mockk.InternalPlatformDsl.toStr
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.generated.enums.Sivilstandstype
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.sivilstand.Sivilstand
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class SivilstandHandlerTest {

    private val sakClient = mockk<SakClient>()
    private val oppgaveClient = mockk<OppgaveClient>()
    private val personhendelseRepository = mockk<PersonhendelseRepository>()
    private val sivilstandHandler = SivilstandHandler(sakClient, oppgaveClient, personhendelseRepository)

    private val personIdent = "12345612344"
    private val partnerPersonIdent = "12345612345"

    @Test
    fun `Ikke opprett oppgave for sivilstand hendelse registrert partner dersom person ikke har løpende ef-sak`() {
        val personhendelse = Personhendelse()
        personhendelse.sivilstand = Sivilstand(
            Sivilstandstype.REGISTRERT_PARTNER.name,
            LocalDate.of(2021, 8, 26),
            partnerPersonIdent,
            LocalDate.of(2021, 8, 26)
        )
        personhendelse.personidenter = listOf(personIdent)
        personhendelse.endringstype = Endringstype.OPPRETTET

        every { sakClient.finnesBehandlingForPerson(personIdent, StønadType.OVERGANGSSTØNAD) } returns false

        val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) } returns 123L

        sivilstandHandler.handle(personhendelse)

        assertThat(oppgaveRequestSlot.isCaptured).isFalse
    }

    @Test
    fun `Opprett oppgave for sivilstand hendelse registrert partner dersom person har løpende ef-sak`() {
        val personhendelse = Personhendelse()
        personhendelse.sivilstand = Sivilstand(
            Sivilstandstype.REGISTRERT_PARTNER.name,
            LocalDate.of(2021, 8, 26),
            partnerPersonIdent,
            LocalDate.of(2021, 8, 26)
        )
        personhendelse.personidenter = listOf(personIdent)
        personhendelse.endringstype = Endringstype.OPPRETTET
        personhendelse.hendelseId = UUID.randomUUID().toString()

        every { sakClient.finnesBehandlingForPerson(personIdent, StønadType.OVERGANGSSTØNAD) } returns true
        every { personhendelseRepository.lagrePersonhendelse(any(), any(), any()) } just runs
        every { personhendelseRepository.lagrePersonhendelse(any(), any(), any()) } just runs

        val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) } returns 123L

        sivilstandHandler.handle(personhendelse)

        assertThat(oppgaveRequestSlot.captured.oppgavetype).isEqualTo(Oppgavetype.VurderLivshendelse)
        assertThat(oppgaveRequestSlot.captured.beskrivelse)
            .isEqualTo("Personhendelse: Sivilstand endret til \"Registrert partner\", gyldig fra og med 26.08.2021")
        assertThat(oppgaveRequestSlot.captured.ident?.ident).isEqualTo(personIdent)
    }

}