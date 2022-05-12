package no.nav.familie.ef.personhendelse.handler

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.pdl.PdlClient
import no.nav.familie.ef.personhendelse.generated.hentperson.Person
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.familie.kontrakter.ef.personhendelse.NyeBarnDto
import no.nav.familie.kontrakter.ef.personhendelse.NyttBarn
import no.nav.familie.kontrakter.ef.personhendelse.NyttBarnÅrsak
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class ForelderBarnHandlerTest {

    private val sakClient = mockk<SakClient>()
    private val pdlClient = mockk<PdlClient>()
    private val person = mockk<Person>()
    private val oppgaveClient = mockk<OppgaveClient>()
    private val personhendelseRepository = mockk<PersonhendelseRepository>(relaxed = true)

    private val handler = ForelderBarnHandler(sakClient)
    private val service = PersonhendelseService(listOf(handler), sakClient, oppgaveClient, personhendelseRepository)

    private val personIdent = "12345612344"

    private val slot = slot<OpprettOppgaveRequest>()

    @BeforeEach
    internal fun setUp() {
        every { oppgaveClient.finnOppgaveMedId(any()) }.returns(Oppgave(id = 0L, status = StatusEnum.OPPRETTET))
        every { sakClient.harLøpendeStønad(any()) } returns true
        every { oppgaveClient.opprettOppgave(capture(slot)) } returns 1L
        every { pdlClient.hentPerson(any()) } returns person
    }

    @Test
    internal fun `finnNyeBarnForBruker inneholder ikke treff, forvent at oppgave ikke opprettes`() {
        val personhendelse = forelderBarnRelasjonHendelse("BARN")
        every { sakClient.finnNyeBarnForBruker(any()) } returns NyeBarnDto(emptyList())
        every { pdlClient.hentPerson(personIdent) } returns person
        service.håndterPersonhendelse(personhendelse)
        verify(exactly = 0) { oppgaveClient.opprettOppgave(any()) }
    }

    @Test
    internal fun `finnNyeBarnForBruker inneholder treff, forvent at oppgave opprettes`() {
        val personhendelse = forelderBarnRelasjonHendelse("BARN")
        every { sakClient.finnNyeBarnForBruker(any()) } returns NyeBarnDto(
            listOf(
                NyttBarn(
                    "fnr",
                    NyttBarnÅrsak.BARN_FINNES_IKKE_PÅ_BEHANDLING
                )
            )
        )
        every { pdlClient.hentPerson(personIdent) } returns person
        service.håndterPersonhendelse(personhendelse)
        verify(exactly = 1) { oppgaveClient.opprettOppgave(any()) }
        assertThat(slot.captured.beskrivelse).isEqualTo("Personhendelse: Bruker har fått et nytt barn.")
    }

    private fun forelderBarnRelasjonHendelse(minRolleForPerson: String): Personhendelse {
        val personhendelse = Personhendelse()
        personhendelse.personidenter = listOf(personIdent)
        personhendelse.opplysningstype = PersonhendelseType.FORELDERBARNRELASJON.hendelsetype
        personhendelse.forelderBarnRelasjon = no.nav.person.pdl.leesah.forelderbarnrelasjon.ForelderBarnRelasjon(
            personIdent,
            "",
            minRolleForPerson,
            null
        )
        personhendelse.hendelseId = UUID.randomUUID().toString()
        personhendelse.endringstype = Endringstype.OPPRETTET
        return personhendelse
    }
}
