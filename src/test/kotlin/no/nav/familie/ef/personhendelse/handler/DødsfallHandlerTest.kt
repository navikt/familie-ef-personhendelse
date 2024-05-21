package no.nav.familie.ef.personhendelse.handler

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.pdl.PdlClient
import no.nav.familie.ef.personhendelse.dødsfalloppgaver.DødsfallOppgaveService
import no.nav.familie.ef.personhendelse.generated.enums.ForelderBarnRelasjonRolle
import no.nav.familie.ef.personhendelse.generated.hentperson.Foedsel
import no.nav.familie.ef.personhendelse.generated.hentperson.ForelderBarnRelasjon
import no.nav.familie.ef.personhendelse.generated.hentperson.Person
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DødsfallHandlerTest {
    private val sakClient = mockk<SakClient>()
    private val pdlClient = mockk<PdlClient>()
    private val oppgaveClient = mockk<OppgaveClient>(relaxed = true)
    private val personhendelseRepository = mockk<PersonhendelseRepository>()
    private val dødsfallOppgaveService = mockk<DødsfallOppgaveService>()

    private val handler = DodsfallHandler(pdlClient)
    private val service = PersonhendelseService(listOf(handler), sakClient, oppgaveClient, personhendelseRepository, dødsfallOppgaveService)

    private val personIdentUtenRelasjoner = "12345612344"
    private val personIdentMor = "12345612345"
    private val personIdentBarn = "01010912345"

    private val personUtenRelasjoner =
        Person(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

    private val personUnder19MedForeldreRelasjoner =
        Person(
            listOf(ForelderBarnRelasjon(personIdentMor, ForelderBarnRelasjonRolle.MOR, ForelderBarnRelasjonRolle.BARN)),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            listOf(Foedsel(foedselsdato = LocalDate.parse("2009-09-09"))),
        )

    @BeforeEach
    fun setup() {
        every { pdlClient.hentPerson(any()) } returns personUtenRelasjoner
        every { pdlClient.hentIdenter(any()) } answers { setOf(firstArg()) }
        every { sakClient.harLøpendeStønad(any()) } returns true
    }

    @Test
    fun `lagre hendelse for senere opprettelse dersom person har sak i ef-sak uavhengig av relasjoner`() {
        val personhendelse = Personhendelse()
        personhendelse.doedsfall = Doedsfall(LocalDate.of(2021, 8, 1))
        personhendelse.opplysningstype = PersonhendelseType.DØDSFALL.hendelsetype
        personhendelse.personidenter = listOf(personIdentUtenRelasjoner)
        personhendelse.hendelseId = UUID.randomUUID().toString()
        personhendelse.endringstype = Endringstype.OPPRETTET

        val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { personhendelseRepository.lagrePersonhendelse(any(), any(), any()) } just runs
        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) } returns 123L
        every { dødsfallOppgaveService.lagreDødsfallOppgave(any(), any(), any(), any()) } just runs

        service.håndterPersonhendelse(personhendelse)

        assertThat(oppgaveRequestSlot.isCaptured).isFalse()
        verify { dødsfallOppgaveService.lagreDødsfallOppgave(any(), any(), any(), any()) }
    }

    @Test
    fun `lagre hendelse for senere opprettelse ved dødsfall for personer under 19 med forelder med løpende ef-sak`() {
        val personhendelse = Personhendelse()
        personhendelse.doedsfall = Doedsfall(LocalDate.of(2021, 8, 1))
        personhendelse.opplysningstype = PersonhendelseType.DØDSFALL.hendelsetype
        personhendelse.personidenter = listOf(personIdentBarn)
        personhendelse.hendelseId = UUID.randomUUID().toString()
        personhendelse.endringstype = Endringstype.OPPRETTET

        every { pdlClient.hentPerson(personIdentBarn) } returns personUnder19MedForeldreRelasjoner
        every { sakClient.harLøpendeStønad(setOf(personIdentBarn)) } returns false
        every { sakClient.harLøpendeStønad(setOf(personIdentMor)) } returns true
        every { personhendelseRepository.lagrePersonhendelse(any(), any(), any()) } just runs
        every { dødsfallOppgaveService.lagreDødsfallOppgave(any(), any(), any(), any()) } just runs

        val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) } returns 123L

        service.håndterPersonhendelse(personhendelse)

        assertThat(oppgaveRequestSlot.isCaptured).isFalse()
        verify { dødsfallOppgaveService.lagreDødsfallOppgave(any(), any(), any(), any()) }
    }
}
