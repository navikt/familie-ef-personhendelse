package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.util.tilNorskDatoformat
import no.nav.person.pdl.leesah.Personhendelse
import org.springframework.stereotype.Component

@Component
class DøfødtBarnHandler(
        sakClient: SakClient,
        oppgaveClient: OppgaveClient
) : PersonhendelseHandler(sakClient, oppgaveClient) {

    override val type = PersonhendelseType.DØDFØDT_BARN

    override fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): String {
        return "Døfødt barn ${personhendelse.doedfoedtBarn.dato.tilNorskDatoformat()}"
    }

}
