package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.defaultOpprettOppgaveRequest
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UtflyttingHandler(val sakClient: SakClient,
                        val oppgaveClient: OppgaveClient) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun handleUtflytting(personhendelse: Personhendelse) {

        personhendelse.utflyttingFraNorge?.let {
            logger.info("Mottatt utflyttingshendelse")
        } ?: throw Exception("Ingen utflyttingFraNorge tilordning i personhendelse : ${personhendelse}")

        val personIdent = personhendelse.personidenter.first()
        val finnesBehandlingForPerson = sakClient.finnesBehandlingForPerson(personIdent, StønadType.OVERGANGSSTØNAD)

        if (finnesBehandlingForPerson) {
            secureLogger.info("Behandling med personIdent finnes : $personIdent : $finnesBehandlingForPerson")
            val beskrivelse = personhendelse.utflyttingsBeskrivelse()
            val request = defaultOpprettOppgaveRequest(personIdent, beskrivelse)
            val oppgaveId = oppgaveClient.opprettOppgave(request)
            secureLogger.info("Oppgave opprettet med oppgaveId: $oppgaveId")
        }
    }
}

private fun Personhendelse.utflyttingsBeskrivelse() = "Utflyttingshendelse til ${this.utflyttingFraNorge.tilflyttingsstedIUtlandet}, " +
                                                      "{${this.utflyttingFraNorge.tilflyttingsland}. " +
                                                      "Utflyttingsdato : ${this.utflyttingFraNorge.utflyttingsdato}"
