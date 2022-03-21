package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.util.identerUtenAktørId
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

// Fjern disse typene
data class NyeBarnDto(val nyeBarn: List<NyttBarn>)

data class NyttBarn(val personIdent: String, val årsak: NyttBarnÅrsak)

enum class NyttBarnÅrsak {
    BARN_FINNES_IKKE_PÅ_BEHANDLING,
    FØDT_FØR_TERMIN
}

@Component
class ForelderBarnHandler(val sakClient: SakClient) : PersonhendelseHandler {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override val type = PersonhendelseType.FORELDERBARNRELASJON

    override fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): OppgaveBeskrivelse {
        val personIdent = personhendelse.identerUtenAktørId().first()
        val nyeBarnForBruker = sakClient.finnNyeBarnForBruker(PersonIdent(personIdent))
        logger.debug("Nye barn for bruker er ${nyeBarnForBruker.nyeBarn.size}, hendelseId : ${personhendelse.hendelseId}")
        if (nyeBarnForBruker.nyeBarn.isNotEmpty()) {
            return OppgaveBeskrivelse(skalOpprettes = false)
        }
        var beskrivelse = "Bruker har fått et nytt barn. "
        if (nyeBarnForBruker.nyeBarn.filter { it.årsak == NyttBarnÅrsak.FØDT_FØR_TERMIN }.isNotEmpty()) {
            beskrivelse += "Et barn er født før termindato."
        }
        return OppgaveBeskrivelse(true, beskrivelse)
    }
}
