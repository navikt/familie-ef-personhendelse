package no.nav.familie.ef.personhendelse.util

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.pdl.secureLogger
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/oppgave"])
@ProtectedWithClaims(issuer = "azuread")
class OppgaveController(
    val oppgaveClient: OppgaveClient,
    val personhendelseRepository: PersonhendelseRepository,
) {
    @PostMapping("/logOpprettedeOppgaver")
    fun logOpprettedeOppgaver() {
        val oppgaveIds = personhendelseRepository.hentAlleOppgaveIds()
        oppgaveIds?.forEach {
            try {
                val oppgave = oppgaveClient.finnOppgaveMedId(it)
                secureLogger.info(
                    "oppgave opprettet fra livshendelse: " + oppgave.beskrivelse +
                        ";status: ${oppgave.status};opprettetTidspunkt: ${oppgave.opprettetTidspunkt}" +
                        ";fnr " + oppgave.identer?.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT },
                )
            } catch (e: Exception) {
                secureLogger.info("Oppgave opprettet fra livshendelse feil:", e)
            }
        }
    }
}
