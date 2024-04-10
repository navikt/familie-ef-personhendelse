package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.datoutil.tilNorskDatoformat
import no.nav.person.pdl.leesah.Personhendelse
import org.springframework.stereotype.Component

@Component
class DødfødtBarnHandler : PersonhendelseHandler {

    override val type = PersonhendelseType.DØDFØDT_BARN

    override fun lagOppgaveInformasjon(personhendelse: Personhendelse): OppgaveInformasjon {
        return UtsettDødsfallOppgave(beskrivelse = "Dødfødt barn ${personhendelse.doedfoedtBarn.dato.tilNorskDatoformat()}")
    }
}
