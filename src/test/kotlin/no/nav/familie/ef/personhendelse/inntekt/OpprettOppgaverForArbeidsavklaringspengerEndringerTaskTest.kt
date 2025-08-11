package no.nav.familie.ef.personhendelse.inntekt

import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.personhendelse.IntegrasjonSpringRunnerTest
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.YearMonth

class OpprettOppgaverForArbeidsavklaringspengerEndringerTaskTest : IntegrasjonSpringRunnerTest() {
    private val inntektOppgaveService = mockk<InntektOppgaveService>(relaxed = true)

    @Autowired
    private lateinit var taskService: TaskService

    private lateinit var opprettOppgaverForArbeidsavklaringspengerEndringerTask: OpprettOppgaverForArbeidsavklaringspengerEndringerTask

    @BeforeEach
    fun setup() {
        opprettOppgaverForArbeidsavklaringspengerEndringerTask =
            OpprettOppgaverForArbeidsavklaringspengerEndringerTask(
                inntektOppgaveService = inntektOppgaveService,
            )
    }

    @Test
    fun `Sjekk at man kan opprette task for arbeidsavklaringspenger endringer og at den har riktig metadata`() {
        val payload =
            PayloadOpprettOppgaverForArbeidsavklaringspengerEndringerTask(
                personIdent = "123",
                årMåned = YearMonth.of(2023, 10),
            )

        val task = OpprettOppgaverForArbeidsavklaringspengerEndringerTask.opprettTask(payload)
        taskService.save(task)
        val taskListOpprettOppgaveTask = taskService.finnAlleTaskerMedType(OpprettOppgaverForArbeidsavklaringspengerEndringerTask.TYPE)
        val taskFraDB = taskListOpprettOppgaveTask.first()
        assertThat(taskFraDB.metadata).isNotEmpty
        assertThat(taskFraDB.metadataWrapper.properties.keys.size).isEqualTo(3)
        assertThat(taskFraDB.metadataWrapper.properties.keys).contains("personIdent", "årMåned", "callId")
        opprettOppgaverForArbeidsavklaringspengerEndringerTask.doTask(task)
        verify(exactly = 1) { inntektOppgaveService.opprettOppgaveForArbeidsavklaringspengerEndring(any(), any()) }
    }
}
