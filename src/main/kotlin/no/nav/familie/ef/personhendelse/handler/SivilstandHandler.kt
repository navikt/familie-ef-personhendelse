package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.defaultOpprettOppgaveRequest
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class SivilstandHandler(
    val sakClient: SakClient,
    val oppgaveClient: OppgaveClient
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun handleSivilstand(personhendelse: Personhendelse) {
        if (personhendelse.sivilstand.relatertVedSivilstand.toString() != "GIFT") {
            return
        }
        logger.info("Mottatt sivilstand hendelse med relatertVedSivilstand = GIFT")

        val personIdent = personhendelse.personidenter.map { it.toString() }.first()
        val finnesBehandlingForPerson = sakClient.finnesBehandlingForPerson(personIdent, StønadType.OVERGANGSSTØNAD)
        if (finnesBehandlingForPerson) {
            secureLogger.info("Finnes behandling med personIdent: $personIdent : $finnesBehandlingForPerson")
            val request = defaultOpprettOppgaveRequest(personIdent, "sivilstand gyldig fra og med ${personhendelse.sivilstand.gyldigFraOgMed}")
            val oppgaveId = oppgaveClient.opprettOppgave(request)
            secureLogger.info("Oppgave opprettet med oppgaveId: $oppgaveId")
        }
    }
}