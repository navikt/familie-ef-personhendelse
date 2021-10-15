package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.pdl.PdlClient
import no.nav.familie.ef.personhendelse.datoutil.tilNorskDatoformat
import no.nav.familie.ef.personhendelse.generated.enums.ForelderBarnRelasjonRolle
import no.nav.familie.ef.personhendelse.util.identerUtenAktørId
import no.nav.person.pdl.leesah.Personhendelse
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class DodsfallHandler(val pdlClient: PdlClient) : PersonhendelseHåndterer {

    override val type = PersonhendelseType.DØDSFALL

    override fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): String {
        return "Dødsfall med dødsdato: ${personhendelse.doedsfall.doedsdato.tilNorskDatoformat()}"
    }

    override fun personidenterPerPersonSomSkalKontrolleres(personhendelse: Personhendelse): List<Set<String>> {
        val personIdenter = personhendelse.identerUtenAktørId()
        val identerTilSøk = mutableListOf(personIdenter)

        val pdlPersonData = pdlClient.hentPerson(personIdenter.first())

        val familierelasjoner = pdlPersonData.forelderBarnRelasjon

        val fødselsdatoList = pdlPersonData.foedsel.mapNotNull { it.foedselsdato?.value }
        if (fødselsdatoList.isEmpty() || fødselsdatoList.first().isAfter(LocalDate.now().minusYears(19))) {
            val identerTilForelderer = familierelasjoner
                    .filter { it.minRolleForPerson == ForelderBarnRelasjonRolle.BARN }
                    .map { it.relatertPersonsIdent }
                    .map { pdlClient.hentIdenter(it) }
                    .filterNot { it.isEmpty() }
            identerTilSøk.addAll(identerTilForelderer)
        }
        return identerTilSøk
    }

}
