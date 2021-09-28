package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import org.springframework.stereotype.Component

@Component
class UtflyttingHandler(
        sakClient: SakClient,
        oppgaveClient: OppgaveClient
) : PersonhendelseHandler(sakClient, oppgaveClient) {

    override val type = PersonhendelseType.UTFLYTTING_FRA_NORGE

    override fun skalOppretteOppgave(personhendelse: Personhendelse): Boolean {
        if (personhendelse.ignorerHendelse()) { // TODO håndter denne
            return false
        }

        personhendelse.utflyttingFraNorge
        ?: throw Exception("Ingen utflyttingFraNorge tilordning i personhendelse=${personhendelse.hendelseId}")
        return true
    }

    override fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): String {
        return "Utflyttingshendelse til ${personhendelse.utflyttingFraNorge.tilflyttingsstedIUtlandet}, " +
               "{${personhendelse.utflyttingFraNorge.tilflyttingsland}. " +
               "Utflyttingsdato: ${personhendelse.utflyttingFraNorge.utflyttingsdato}"
    }

    private val ignorteEndringstyper = listOf(Endringstype.ANNULLERT, Endringstype.KORRIGERT)

    private fun Personhendelse.ignorerHendelse() = ignorteEndringstyper.contains(this.endringstype)
}
