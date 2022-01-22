package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.pdl.PdlClient
import no.nav.familie.ef.personhendelse.generated.enums.ForelderBarnRelasjonRolle
import no.nav.familie.ef.personhendelse.util.identerUtenAktørId
import no.nav.person.pdl.leesah.Personhendelse
import org.springframework.stereotype.Component

@Component
class ForelderBarnHandler(val sakClient: SakClient, val pdlClient: PdlClient) : PersonhendelseHandler {

    override val type = PersonhendelseType.FORELDERBARNRELASJON

    override fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): String {
        return "Bruker har fått et nytt barn"
    }

    override fun skalOppretteOppgave(personhendelse: Personhendelse): Boolean {
        val personIdent = personhendelse.identerUtenAktørId().first()
        val antallBarnPdl =
            pdlClient.hentPerson(personIdent).forelderBarnRelasjon.filter { it.minRolleForPerson == ForelderBarnRelasjonRolle.BARN }.size
        val antallBarnSøknad = sakClient.hentFnrForAlleBarn(personIdent).size
        return antallBarnSøknad != antallBarnPdl
    }
}
