package no.nav.familie.ef.personhendelse.forsinketoppgave

import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.ef.personhendelse.handler.PersonhendelseType
import no.nav.person.pdl.leesah.Endringstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

class ForsinketOppgaveRepositoryTest : IntegrasjonSpringRunnerTest() {
    @Autowired
    lateinit var forsinketOppgaveRepository: ForsinketOppgaveRepository

    @Test
    fun `lagre og hent forsinket oppgave, forvent like felter`() {
        val `forsinkede oppgaver` =
            ForsinketOppgave(
                UUID.randomUUID(),
                "123",
                "beskrivelse",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )

        lagreOppgave(`forsinkede oppgaver`)

        val forsinkedeOppgaver = forsinketOppgaveRepository.hentIkkeOpprettedeForsinkedeOppgaverOverEnUkeTilbakeITid()

        assertThat(forsinkedeOppgaver.first().hendelsesId).isEqualTo(`forsinkede oppgaver`.hendelsesId)
        assertThat(forsinkedeOppgaver.first().personId).isEqualTo(`forsinkede oppgaver`.personId)
        assertThat(forsinkedeOppgaver.first().personhendelseType).isEqualTo(`forsinkede oppgaver`.personhendelseType)
        assertThat(forsinkedeOppgaver.first().beskrivelse).isEqualTo(`forsinkede oppgaver`.beskrivelse)
        assertThat(forsinkedeOppgaver.first().endringstype).isEqualTo(`forsinkede oppgaver`.endringstype)
        assertThat(forsinkedeOppgaver.first().hendelsesTid).isNotNull()
        assertThat(forsinkedeOppgaver.first().opprettetOppgaveTid).isNull()
    }

    @Test
    fun `lagre og hent flere forsinkede oppgaver`() {
        val forsinketOppgave =
            ForsinketOppgave(
                UUID.randomUUID(),
                "123",
                "beskrivelse",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )
        val forsinketOppgave2 =
            ForsinketOppgave(
                UUID.randomUUID(),
                "456",
                "beskrivelse2",
                PersonhendelseType.DØDSFALL,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )
        val forsinketOppgave3 =
            ForsinketOppgave(
                UUID.randomUUID(),
                "456",
                "beskrivelse2",
                PersonhendelseType.FORELDERBARNRELASJON,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )
        lagreOppgave(forsinketOppgave)
        lagreOppgave(forsinketOppgave2)
        lagreOppgave(forsinketOppgave3)

        val forsinkedeOppgaver = forsinketOppgaveRepository.hentIkkeOpprettedeForsinkedeOppgaverOverEnUkeTilbakeITid()

        assertThat(forsinkedeOppgaver).hasSize(3)
    }

    @Test
    fun `lagre og hent kun forsinkede oppgaver som er over en uke gammel`() {
        val `forsinkede oppgaver` =
            ForsinketOppgave(
                UUID.randomUUID(),
                "123",
                "beskrivelse",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )
        val forsinketOppgave2 =
            ForsinketOppgave(
                UUID.randomUUID(),
                "456",
                "beskrivelse2",
                PersonhendelseType.FORELDERBARNRELASJON,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(2),
                null,
            )
        val forsinketOppgave3 =
            ForsinketOppgave(
                UUID.randomUUID(),
                "456",
                "beskrivelse3",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now(),
                null,
            )

        lagreOppgave(`forsinkede oppgaver`)
        lagreOppgave(forsinketOppgave2)
        lagreOppgave(forsinketOppgave3)

        val forsinkedeOppgaver = forsinketOppgaveRepository.hentIkkeOpprettedeForsinkedeOppgaverOverEnUkeTilbakeITid()

        assertThat(forsinkedeOppgaver).hasSize(2)
        assertThat(forsinkedeOppgaver.firstOrNull { it.hendelsesId == `forsinkede oppgaver`.hendelsesId }).isNotNull()
        assertThat(forsinkedeOppgaver.firstOrNull { it.hendelsesId == forsinketOppgave2.hendelsesId }).isNotNull()
    }

    @Test
    fun `lagre og hent kun forsinkede oppgaver som ikke har blitt utført`() {
        val forsinketOppgaveIkkeOpprettet =
            ForsinketOppgave(
                UUID.randomUUID(),
                "123",
                "beskrivelse",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )
        val forsinketOppgaveOpprettet =
            ForsinketOppgave(
                UUID.randomUUID(),
                "456",
                "beskrivelse2",
                PersonhendelseType.DØDSFALL,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1),
                null,
            )

        lagreOppgave(forsinketOppgaveIkkeOpprettet)
        lagreOppgave(forsinketOppgaveOpprettet)
        forsinketOppgaveRepository.settOppgaveTilUtført(forsinketOppgaveOpprettet.hendelsesId)

        val forsinkedeOppgaver = forsinketOppgaveRepository.hentIkkeOpprettedeForsinkedeOppgaverOverEnUkeTilbakeITid()

        assertThat(forsinkedeOppgaver).hasSize(1)
        assertThat(forsinkedeOppgaver.first().hendelsesId).isEqualTo(forsinketOppgaveIkkeOpprettet.hendelsesId)
    }

    @Test
    fun `ikke lagre duplikate forsinkede oppgaver`() {
        val forsinketOppgave =
            ForsinketOppgave(
                UUID.randomUUID(),
                "123",
                "beskrivelse",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )

        lagreOppgave(forsinketOppgave)
        lagreOppgave(forsinketOppgave)

        val forsinkedeOppgaver = forsinketOppgaveRepository.hentIkkeOpprettedeForsinkedeOppgaverOverEnUkeTilbakeITid()

        assertThat(forsinkedeOppgaver).hasSize(1)
    }

    private fun lagreOppgave(forsinketOppgave: ForsinketOppgave) {
        forsinketOppgaveRepository.lagreOppgave(
            forsinketOppgave.hendelsesId,
            forsinketOppgave.personhendelseType,
            forsinketOppgave.endringstype.name,
            forsinketOppgave.personId,
            forsinketOppgave.beskrivelse,
            forsinketOppgave.hendelsesTid,
        )
    }
}
