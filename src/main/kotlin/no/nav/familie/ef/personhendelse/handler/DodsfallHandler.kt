package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.pdl.PdlClient
import no.nav.familie.ef.personhendelse.datoutil.tilNorskDatoformat
import no.nav.familie.ef.personhendelse.generated.enums.ForelderBarnRelasjonRolle
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.person.pdl.leesah.Personhendelse
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class DodsfallHandler(
        val pdlClient: PdlClient,
        sakClient: SakClient,
        oppgaveClient: OppgaveClient,
        personhendelseRepository: PersonhendelseRepository
) : PersonhendelseHandler(sakClient, oppgaveClient, personhendelseRepository) {

    override val type = PersonhendelseType.DØDSFALL

    override fun handle(personhendelse: Personhendelse) {
        identerTilSøk(personhendelse).forEach { personIdent ->
            // TODO fjern stønadType som i PersonhendelseHandler
            val finnesBehandlingForPerson = sakClient.finnesBehandlingForPerson(personIdent, StønadType.OVERGANGSSTØNAD)

            if (finnesBehandlingForPerson) {
                handlePersonhendelse(personhendelse, personIdent)
            }
        }
    }

    override fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): String {
        return "Dødsfall med dødsdato: ${personhendelse.doedsfall.doedsdato.tilNorskDatoformat()}"
    }

    private fun identerTilSøk(personhendelse: Personhendelse): List<String> {
        val personIdent = personhendelse.personidenter.first()
        val identerTilSøk = mutableListOf(personIdent)

        val pdlPersonData = pdlClient.hentPerson(personIdent)

        val familierelasjoner = pdlPersonData.forelderBarnRelasjon

        val fødselsdatoList = pdlPersonData.foedsel.mapNotNull { it.foedselsdato?.value }
        if (fødselsdatoList.isEmpty() || fødselsdatoList.first().isAfter(LocalDate.now().minusYears(19))) {
            val listeMedForeldreForBarn =
                    familierelasjoner.filter { it.minRolleForPerson == ForelderBarnRelasjonRolle.BARN }
                            .map { it.relatertPersonsIdent }
            identerTilSøk.addAll(listeMedForeldreForBarn)
        }
        return identerTilSøk
    }

}