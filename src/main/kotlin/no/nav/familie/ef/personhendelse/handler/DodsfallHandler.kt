package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.PdlClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.defaultOpprettOppgaveRequest
import no.nav.familie.ef.personhendelse.generated.enums.ForelderBarnRelasjonRolle
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.*

@Component
class DodsfallHandler(
    val sakClient: SakClient,
    val pdlClient: PdlClient,
    val oppgaveClient: OppgaveClient
) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun handleDodsfall(personhendelse: Personhendelse) {

        identerTilSøk(personhendelse).forEach {
            val finnesBehandlingForPerson = sakClient.finnesBehandlingForPerson(it, StønadType.OVERGANGSSTØNAD)
            secureLogger.info("Finnes behandling med personIdent: $it : $finnesBehandlingForPerson")
            if (finnesBehandlingForPerson) {
                val opprettOppgaveRequest =
                    defaultOpprettOppgaveRequest(it, "dødsfall med dødsdato: ${personhendelse.doedsfall.doedsdato}")
                val oppgaveId = oppgaveClient.opprettOppgave(opprettOppgaveRequest)
                secureLogger.info("Oppgave opprettet med oppgaveId: $oppgaveId")
            }
        }
    }

    private fun identerTilSøk(personhendelse: Personhendelse): List<String> {
        val identerTilSøk = mutableListOf<String>()
        val personIdent = personhendelse.personidenter.map { it.toString() }.first()

        val callId = UUID.randomUUID().toString()
        val pdlPersonData = pdlClient.hentPerson(personIdent, callId).data

        val familierelasjoner = pdlPersonData?.hentPerson?.forelderBarnRelasjon
        val listeMedBarn = familierelasjoner?.filter { it.minRolleForPerson != ForelderBarnRelasjonRolle.BARN }
            ?.map { it.relatertPersonsIdent } ?: emptyList()
        identerTilSøk.addAll(listeMedBarn)

        val fødselsdatoList = pdlPersonData?.hentPerson?.foedsel?.map { LocalDate.parse(it.foedselsdato.toString()) }
        if (fødselsdatoList.isNullOrEmpty() ||
            fødselsdatoList.first().isAfter(LocalDate.now().minusYears(19))
        ) {
            val listeMedForeldreForBarn =
                familierelasjoner?.filter { it.minRolleForPerson == ForelderBarnRelasjonRolle.BARN }
                    ?.map { it.relatertPersonsIdent } ?: emptyList()
            identerTilSøk.addAll(listeMedForeldreForBarn)
        }
        return identerTilSøk
    }

}