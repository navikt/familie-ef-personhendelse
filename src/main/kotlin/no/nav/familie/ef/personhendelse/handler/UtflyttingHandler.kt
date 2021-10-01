package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.personhendelsemapping.PersonhendelseRepository
import no.nav.person.pdl.leesah.Personhendelse
import org.springframework.stereotype.Component

@Component
class UtflyttingHandler(
    sakClient: SakClient,
    oppgaveClient: OppgaveClient,
    personhendelseRepository: PersonhendelseRepository
) : PersonhendelseHandler(sakClient, oppgaveClient, personhendelseRepository) {

    override val type = PersonhendelseType.UTFLYTTING_FRA_NORGE

    override fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): String {
        return personhendelse.utflyttingsBeskrivelse()
    }

    override fun skalOppretteOppgave(personhendelse: Personhendelse): Boolean {
        personhendelse.utflyttingFraNorge?.let {
            logger.info("Mottatt utflyttingshendelse")
        } ?: throw Exception("Ingen utflyttingFraNorge tilordning i personhendelse : ${personhendelse}")
        return true
    }
}

private fun Personhendelse.utflyttingsBeskrivelse() =
    "Utflyttingshendelse til ${this.utflyttingFraNorge.tilflyttingsstedIUtlandet}, " +
            "{${this.utflyttingFraNorge.tilflyttingsland}. " +
            "Utflyttingsdato : ${this.utflyttingFraNorge.utflyttingsdato}"



