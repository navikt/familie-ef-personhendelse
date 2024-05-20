package no.nav.familie.ef.personhendelse.dødsfalloppgaver

import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.ef.personhendelse.handler.PersonhendelseType
import no.nav.person.pdl.leesah.Endringstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

class DødsfallOppgaveRepositoryTest : IntegrasjonSpringRunnerTest() {
    @Autowired
    lateinit var dødsfallOppgaveRepository: DødsfallOppgaveRepository

    @Test
    fun `lagre og hent dødsfalloppgave, forvent like felter`() {
        val dødsfallOppgave =
            DødsfallOppgave(
                UUID.randomUUID(),
                "123",
                "beskrivelse",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )

        lagreOppgave(dødsfallOppgave)

        val dødsfallOppgaver = dødsfallOppgaveRepository.hentIkkeOpprettedeDødsfalloppgaverOverEnUkeTilbakeITid()

        assertThat(dødsfallOppgaver.first().hendelsesId).isEqualTo(dødsfallOppgave.hendelsesId)
        assertThat(dødsfallOppgaver.first().personId).isEqualTo(dødsfallOppgave.personId)
        assertThat(dødsfallOppgaver.first().personhendelseType).isEqualTo(dødsfallOppgave.personhendelseType)
        assertThat(dødsfallOppgaver.first().beskrivelse).isEqualTo(dødsfallOppgave.beskrivelse)
        assertThat(dødsfallOppgaver.first().endringstype).isEqualTo(dødsfallOppgave.endringstype)
        assertThat(dødsfallOppgaver.first().hendelsesTid).isNotNull()
        assertThat(dødsfallOppgaver.first().opprettetOppgaveTid).isNull()
    }

    @Test
    fun `lagre og hent flere dødsfallOppgaver`() {
        val dødsfallOppgave =
            DødsfallOppgave(
                UUID.randomUUID(),
                "123",
                "beskrivelse",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )
        val dødsfallOppgave2 =
            DødsfallOppgave(
                UUID.randomUUID(),
                "456",
                "beskrivelse2",
                PersonhendelseType.DØDSFALL,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )

        lagreOppgave(dødsfallOppgave)
        lagreOppgave(dødsfallOppgave2)

        val dødsfallOppgaver = dødsfallOppgaveRepository.hentIkkeOpprettedeDødsfalloppgaverOverEnUkeTilbakeITid()

        assertThat(dødsfallOppgaver).hasSize(2)
    }

    @Test
    fun `lagre og hent kun dødsfallOppgaver som er over en uke gammel`() {
        val dødsfallOppgave =
            DødsfallOppgave(
                UUID.randomUUID(),
                "123",
                "beskrivelse",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )
        val dødsfallOppgave2 =
            DødsfallOppgave(
                UUID.randomUUID(),
                "456",
                "beskrivelse2",
                PersonhendelseType.DØDSFALL,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(2),
                null,
            )
        val dødsfallOppgave3 =
            DødsfallOppgave(
                UUID.randomUUID(),
                "456",
                "beskrivelse3",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now(),
                null,
            )

        lagreOppgave(dødsfallOppgave)
        lagreOppgave(dødsfallOppgave2)
        lagreOppgave(dødsfallOppgave3)

        val dødsfallOppgaver = dødsfallOppgaveRepository.hentIkkeOpprettedeDødsfalloppgaverOverEnUkeTilbakeITid()

        assertThat(dødsfallOppgaver).hasSize(2)
        assertThat(dødsfallOppgaver.firstOrNull { it.hendelsesId == dødsfallOppgave.hendelsesId }).isNotNull()
        assertThat(dødsfallOppgaver.firstOrNull { it.hendelsesId == dødsfallOppgave2.hendelsesId }).isNotNull()
    }

    @Test
    fun `lagre og hent kun dødsfallOppgaver som ikke har blitt utført`() {
        val dødsfallOppgaveIkkeOpprettet =
            DødsfallOppgave(
                UUID.randomUUID(),
                "123",
                "beskrivelse",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )
        val dødsfallOppgaveOpprettet =
            DødsfallOppgave(
                UUID.randomUUID(),
                "456",
                "beskrivelse2",
                PersonhendelseType.DØDSFALL,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1),
                null,
            )

        lagreOppgave(dødsfallOppgaveIkkeOpprettet)
        lagreOppgave(dødsfallOppgaveOpprettet)
        dødsfallOppgaveRepository.settOppgaveTilUtført(dødsfallOppgaveOpprettet.hendelsesId)

        val dødsfallOppgaver = dødsfallOppgaveRepository.hentIkkeOpprettedeDødsfalloppgaverOverEnUkeTilbakeITid()

        assertThat(dødsfallOppgaver).hasSize(1)
        assertThat(dødsfallOppgaver.first().hendelsesId).isEqualTo(dødsfallOppgaveIkkeOpprettet.hendelsesId)
    }

    @Test
    fun `ikke lagre duplikate dødsfallOppgaver`() {
        val dødsfallOppgave =
            DødsfallOppgave(
                UUID.randomUUID(),
                "123",
                "beskrivelse",
                PersonhendelseType.DØDFØDT_BARN,
                Endringstype.OPPRETTET,
                LocalDateTime.now().minusWeeks(1).minusDays(1),
                null,
            )

        lagreOppgave(dødsfallOppgave)
        lagreOppgave(dødsfallOppgave)

        val dødsfallOppgaver = dødsfallOppgaveRepository.hentIkkeOpprettedeDødsfalloppgaverOverEnUkeTilbakeITid()

        assertThat(dødsfallOppgaver).hasSize(1)
    }

    private fun lagreOppgave(dødsfallOppgave: DødsfallOppgave) {
        dødsfallOppgaveRepository.lagreOppgave(
            dødsfallOppgave.hendelsesId,
            dødsfallOppgave.personhendelseType,
            dødsfallOppgave.endringstype.name,
            dødsfallOppgave.personId,
            dødsfallOppgave.beskrivelse,
            dødsfallOppgave.hendelsesTid,
        )
    }
}
