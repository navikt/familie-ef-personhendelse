package no.nav.familie.ef.personhendelse.utsattoppgave

import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.ef.personhendelse.handler.PersonhendelseType
import no.nav.person.pdl.leesah.Endringstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

class UtsattOppgaveRepositoryTest : IntegrasjonSpringRunnerTest() {
    @Autowired
    lateinit var utsattOppgaveRepository: UtsattOppgaveRepository

    @Test
    fun `lagre og hent utsatt oppgave, forvent like felter`() {
        val utsattOppgave =
            UtsattOppgave(
                UUID.randomUUID(),
                "123",
                "beskrivelse",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )

        lagreOppgave(utsattOppgave)

        val forsinkedeOppgaver = utsattOppgaveRepository.hentIkkeOpprettedeForsinkedeOppgaverOverEnUkeTilbakeITid()

        assertThat(forsinkedeOppgaver.first().hendelsesId).isEqualTo(utsattOppgave.hendelsesId)
        assertThat(forsinkedeOppgaver.first().personId).isEqualTo(utsattOppgave.personId)
        assertThat(forsinkedeOppgaver.first().personhendelseType).isEqualTo(utsattOppgave.personhendelseType)
        assertThat(forsinkedeOppgaver.first().beskrivelse).isEqualTo(utsattOppgave.beskrivelse)
        assertThat(forsinkedeOppgaver.first().endringstype).isEqualTo(utsattOppgave.endringstype)
        assertThat(forsinkedeOppgaver.first().hendelsesTid).isNotNull()
        assertThat(forsinkedeOppgaver.first().opprettetOppgaveTid).isNull()
    }

    @Test
    fun `lagre og hent flere forsinkede oppgaver`() {
        val utsattOppgave =
            UtsattOppgave(
                UUID.randomUUID(),
                "123",
                "beskrivelse",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )
        val utsattOppgave2 =
            UtsattOppgave(
                UUID.randomUUID(),
                "456",
                "beskrivelse2",
                PersonhendelseType.DØDSFALL,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )
        val utsattOppgave3 =
            UtsattOppgave(
                UUID.randomUUID(),
                "456",
                "beskrivelse2",
                PersonhendelseType.FORELDERBARNRELASJON,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )
        lagreOppgave(utsattOppgave)
        lagreOppgave(utsattOppgave2)
        lagreOppgave(utsattOppgave3)

        val forsinkedeOppgaver = utsattOppgaveRepository.hentIkkeOpprettedeForsinkedeOppgaverOverEnUkeTilbakeITid()

        assertThat(forsinkedeOppgaver).hasSize(3)
    }

    @Test
    fun `lagre og hent kun forsinkede oppgaver som er over en uke gammel`() {
        val utsattOppgave =
            UtsattOppgave(
                UUID.randomUUID(),
                "123",
                "beskrivelse",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )
        val utsattOppgave2 =
            UtsattOppgave(
                UUID.randomUUID(),
                "456",
                "beskrivelse2",
                PersonhendelseType.FORELDERBARNRELASJON,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(2),
                null,
            )
        val utsattOppgave3 =
            UtsattOppgave(
                UUID.randomUUID(),
                "456",
                "beskrivelse3",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now(),
                null,
            )

        lagreOppgave(utsattOppgave)
        lagreOppgave(utsattOppgave2)
        lagreOppgave(utsattOppgave3)

        val forsinkedeOppgaver = utsattOppgaveRepository.hentIkkeOpprettedeForsinkedeOppgaverOverEnUkeTilbakeITid()

        assertThat(forsinkedeOppgaver).hasSize(2)
        assertThat(forsinkedeOppgaver.firstOrNull { it.hendelsesId == utsattOppgave.hendelsesId }).isNotNull()
        assertThat(forsinkedeOppgaver.firstOrNull { it.hendelsesId == utsattOppgave2.hendelsesId }).isNotNull()
    }

    @Test
    fun `lagre og hent kun forsinkede oppgaver som ikke har blitt utført`() {
        val utsattOppgaveIkkeOpprettet =
            UtsattOppgave(
                UUID.randomUUID(),
                "123",
                "beskrivelse",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )
        val utsattOppgaveOpprettet =
            UtsattOppgave(
                UUID.randomUUID(),
                "456",
                "beskrivelse2",
                PersonhendelseType.DØDSFALL,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1),
                null,
            )

        lagreOppgave(utsattOppgaveIkkeOpprettet)
        lagreOppgave(utsattOppgaveOpprettet)
        utsattOppgaveRepository.settOppgaveTilUtført(utsattOppgaveOpprettet.hendelsesId)

        val forsinkedeOppgaver = utsattOppgaveRepository.hentIkkeOpprettedeForsinkedeOppgaverOverEnUkeTilbakeITid()

        assertThat(forsinkedeOppgaver).hasSize(1)
        assertThat(forsinkedeOppgaver.first().hendelsesId).isEqualTo(utsattOppgaveIkkeOpprettet.hendelsesId)
    }

    @Test
    fun `ikke lagre duplikate forsinkede oppgaver`() {
        val utsattOppgave =
            UtsattOppgave(
                UUID.randomUUID(),
                "123",
                "beskrivelse",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )

        lagreOppgave(utsattOppgave)
        lagreOppgave(utsattOppgave)

        val forsinkedeOppgaver = utsattOppgaveRepository.hentIkkeOpprettedeForsinkedeOppgaverOverEnUkeTilbakeITid()

        assertThat(forsinkedeOppgaver).hasSize(1)
    }

    private fun lagreOppgave(utsattOppgave: UtsattOppgave) {
        utsattOppgaveRepository.lagreOppgave(
            utsattOppgave.hendelsesId,
            utsattOppgave.personhendelseType,
            utsattOppgave.endringstype.name,
            utsattOppgave.personId,
            utsattOppgave.beskrivelse,
            utsattOppgave.hendelsesTid,
        )
    }
}
