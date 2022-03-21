package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.datoutil.tilNorskDatoformat
import no.nav.person.pdl.leesah.Personhendelse
import org.springframework.stereotype.Component

@Component
class UtflyttingHandler : PersonhendelseHandler {

    override val type = PersonhendelseType.UTFLYTTING_FRA_NORGE

    override fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): OppgaveBeskrivelse {
        return OppgaveBeskrivelse(skalOpprettes = true, personhendelse.utflyttingsBeskrivelse())
    }
}

private fun Personhendelse.utflyttingsBeskrivelse() =
    "Utflyttingshendelse til ${this.utflyttingFraNorge.tilflyttingsstedIUtlandet}, " +
        "{${this.utflyttingFraNorge.tilflyttingsland}. " +
        "Utflyttingsdato: ${this.utflyttingFraNorge.utflyttingsdato.tilNorskDatoformat()}"
