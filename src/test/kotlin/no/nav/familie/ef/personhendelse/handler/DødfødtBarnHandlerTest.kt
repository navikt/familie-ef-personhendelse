package no.nav.familie.ef.personhendelse.handler

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.dødsfalloppgaver.DødsfallOppgaveService
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.doedfoedtbarn.DoedfoedtBarn
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class DødfødtBarnHandlerTest {

    private val sakClient = mockk<SakClient>()
    private val oppgaveClient = mockk<OppgaveClient>(relaxed = true)
    private val personhendelseRepository = mockk<PersonhendelseRepository>(relaxed = true)
    private val dødsfallOppgaveService = mockk<DødsfallOppgaveService>()

    private val handler = DødfødtBarnHandler()
    private val service = PersonhendelseService(listOf(handler), sakClient, oppgaveClient, personhendelseRepository, dødsfallOppgaveService)

    private val personIdent = "12345612344"

    private val slot = slot<String>()

    @BeforeEach
    internal fun setUp() {
        every { sakClient.harLøpendeStønad(any()) } returns true
        every { dødsfallOppgaveService.lagreDødsfallOppgave(any(), any(), any(), capture(slot)) } just runs
    }

    @Test
    internal fun `skal lagre hendelse for dødfødt barn uten dato med beskrivelse for at det ikke finnes dato`() {
        val personhendelse = dødfødtBarn(null)

        service.håndterPersonhendelse(personhendelse)

        verify(exactly = 0) { oppgaveClient.opprettOppgave(any()) }
        verify { dødsfallOppgaveService.lagreDødsfallOppgave(any(), any(), any(), any()) }
        Assertions.assertThat(slot.isCaptured).isTrue()
        Assertions.assertThat(slot.captured).contains("Dødfødt barn ukjent dato")
    }

    @Test
    internal fun `skal lagre hendelse for dødfødt barn med dato`() {
        val personhendelse = dødfødtBarn(LocalDate.of(2021, 10, 1))

        service.håndterPersonhendelse(personhendelse)

        verify(exactly = 0) { oppgaveClient.opprettOppgave(any()) }
        verify { dødsfallOppgaveService.lagreDødsfallOppgave(any(), any(), any(), any()) }
        Assertions.assertThat(slot.isCaptured).isTrue()
        Assertions.assertThat(slot.captured).contains("Dødfødt barn 01.10.2021")
    }

    private fun dødfødtBarn(dato: LocalDate?): Personhendelse {
        val personhendelse = Personhendelse()
        personhendelse.personidenter = listOf(personIdent)
        personhendelse.opplysningstype = PersonhendelseType.DØDFØDT_BARN.hendelsetype
        personhendelse.hendelseId = UUID.randomUUID().toString()
        personhendelse.doedfoedtBarn = DoedfoedtBarn(dato)
        personhendelse.endringstype = Endringstype.OPPRETTET
        return personhendelse
    }
}
