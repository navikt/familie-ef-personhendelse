package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class LoadHandlersTest : IntegrasjonSpringRunnerTest() {

    @Autowired
    private lateinit var handlers: List<PersonhendelseHandler>

    @Test
    internal fun `skal laste inn handlers`() {
        assertThat(handlers).isNotEmpty
    }
}