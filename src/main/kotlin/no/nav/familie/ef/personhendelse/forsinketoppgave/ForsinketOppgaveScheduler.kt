package no.nav.familie.ef.personhendelse.forsinketoppgave

import no.nav.familie.ef.personhendelse.handler.PersonhendelseService
import no.nav.familie.leader.LeaderClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ForsinketOppgaveScheduler(
    val personhendelseService: PersonhendelseService,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "\${OPPGAVERDØDSFALL_CRON_EXPRESSION}")
    fun opprettForsinkedeOppgaver() {
        if (LeaderClient.isLeader() == true) {
            logger.info("Cron scheduler starter for opprettelse av oppgaver ifm dødsfall")
            personhendelseService.opprettOppgaverAvUkesgamleHendelser()
        }
    }
}
