package no.nav.familie.ef.personhendelse.handler

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.PdlClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.generated.HentPerson
import no.nav.familie.ef.personhendelse.generated.enums.ForelderBarnRelasjonRolle
import no.nav.familie.ef.personhendelse.generated.hentperson.Foedsel
import no.nav.familie.ef.personhendelse.generated.hentperson.ForelderBarnRelasjon
import no.nav.familie.ef.personhendelse.generated.hentperson.Person
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate



class DodsfallHandlerTest {

    private val sakClient = mockk<SakClient>()
    private val pdlClient = mockk<PdlClient>()
    private val oppgaveClient = mockk<OppgaveClient>()


    private val dodsfallHandler = DodsfallHandler(sakClient, pdlClient, oppgaveClient)

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
            listOf(Foedsel(foedselsdato = "2009-09-09"))
        )

    @BeforeEach
    fun setup() {
        every { pdlClient.hentPerson(any()) } returns personUtenRelasjoner
        every { sakClient.finnesBehandlingForPerson(personIdentUtenRelasjoner, StønadType.OVERGANGSSTØNAD) } returns true

    }

    @Test
    fun `Opprett oppgave dersom person har sak i ef-sak uavhengig av relasjoner`() {
        val personhendelse = Personhendelse()
        personhendelse.doedsfall = Doedsfall(LocalDate.of(2021, 8, 1))
        personhendelse.personidenter = listOf(personIdentUtenRelasjoner)

        val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) } returns 123L

        dodsfallHandler.handleDodsfall(personhendelse)

        assertThat(oppgaveRequestSlot.captured.oppgavetype).isEqualTo(Oppgavetype.VurderLivshendelse)
        assertThat(oppgaveRequestSlot.captured.beskrivelse).isEqualTo("Personhendelse: Dødsfall med dødsdato 01.08.2021")
        assertThat(oppgaveRequestSlot.captured.ident?.ident).isEqualTo(personIdentUtenRelasjoner)
    }

    @Test
    fun `Opprett oppgave ved dødsfall for personer under 19 med forelder med løpende ef-sak`() {
        val personhendelse = Personhendelse()
        personhendelse.doedsfall = Doedsfall(LocalDate.of(2021, 8, 1))
        personhendelse.personidenter = listOf(personIdentBarn)

        every { pdlClient.hentPerson(personIdentBarn) } returns personUnder19MedForeldreRelasjoner
        every { sakClient.finnesBehandlingForPerson(personIdentBarn, StønadType.OVERGANGSSTØNAD) } returns false
        every { sakClient.finnesBehandlingForPerson(personIdentMor, StønadType.OVERGANGSSTØNAD) } returns true

        val oppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(oppgaveRequestSlot)) } returns 123L

        dodsfallHandler.handleDodsfall(personhendelse)

        assertThat(oppgaveRequestSlot.captured.oppgavetype).isEqualTo(Oppgavetype.VurderLivshendelse)
        assertThat(oppgaveRequestSlot.captured.beskrivelse).isEqualTo("Personhendelse: Dødsfall med dødsdato 01.08.2021")
        assertThat(oppgaveRequestSlot.captured.ident?.ident).isEqualTo(personIdentMor)
    }
}