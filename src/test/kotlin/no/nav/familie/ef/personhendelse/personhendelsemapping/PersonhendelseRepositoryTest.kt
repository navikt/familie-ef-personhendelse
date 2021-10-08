package no.nav.familie.ef.personhendelse.personhendelsemapping

import no.nav.familie.ef.personhendelse.Hendelse
import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.person.pdl.leesah.Endringstype
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

internal class PersonhendelseRepositoryTest : IntegrasjonSpringRunnerTest() {

    @Autowired lateinit var personhendelseRepository: PersonhendelseRepository

    private val hendelse = Hendelse(
        hendelsesId = UUID.randomUUID(),
        oppgaveId = 1L,
        endringstype = Endringstype.OPPRETTET,
        opprettetTid = LocalDateTime.now()
    )

    @BeforeEach
    internal fun beforeEach() {
        personhendelseRepository.lagrePersonhendelse(hendelse.hendelsesId, hendelse.oppgaveId, hendelse.endringstype)
    }

    @Test
    fun `Hent lagret personhendelse for UUID, forvent likhet`() {
        val hentetHendelse = personhendelseRepository.hentHendelse(hendelse.hendelsesId)!!
        Assertions.assertThat(hendelse.oppgaveId).isEqualTo(hentetHendelse.oppgaveId)
        Assertions.assertThat(hendelse.hendelsesId).isEqualTo(hentetHendelse.hendelsesId)
        Assertions.assertThat(hendelse.endringstype).isEqualTo(hentetHendelse.endringstype)
    }

}