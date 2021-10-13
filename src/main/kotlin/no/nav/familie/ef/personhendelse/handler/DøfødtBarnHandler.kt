package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.datoutil.tilNorskDatoformat
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.person.pdl.leesah.Personhendelse
import org.springframework.stereotype.Component

@Component
class DøfødtBarnHandler(
        sakClient: SakClient,
        oppgaveClient: OppgaveClient,
        personhendelseRepository: PersonhendelseRepository
) : PersonhendelseHandler(sakClient, oppgaveClient, personhendelseRepository) {

    override val type = PersonhendelseType.DØDFØDT_BARN

    override fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): String {
        return "Døfødt barn ${personhendelse.doedfoedtBarn.dato.tilNorskDatoformat()}"
    }

}
