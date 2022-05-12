package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.util.identerUtenAktørId
import no.nav.familie.kontrakter.ef.personhendelse.NyttBarn
import no.nav.familie.kontrakter.ef.personhendelse.NyttBarnÅrsak
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ForelderBarnHandler(val sakClient: SakClient) : PersonhendelseHandler {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override val type = PersonhendelseType.FORELDERBARNRELASJON

    override fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): OppgaveInformasjon {
        val personIdent = personhendelse.identerUtenAktørId().first()
        val nyeBarnForBruker = sakClient.finnNyeBarnForBruker(PersonIdent(personIdent))
        if (nyeBarnForBruker.nyeBarn.isEmpty()) {
            return IkkeOpprettOppgave
        }

        logger.info("Nye barn for bruker er ${nyeBarnForBruker.nyeBarn}, hendelseId : ${personhendelse.hendelseId}")

        val barnFødtFørTermin = nyeBarnForBruker.nyeBarn.filter { it.årsak == NyttBarnÅrsak.FØDT_FØR_TERMIN }
        val nyttBarn = nyeBarnForBruker.nyeBarn.filter { it.årsak == NyttBarnÅrsak.BARN_FINNES_IKKE_PÅ_BEHANDLING }
        if (barnFødtFørTermin.isNotEmpty()) {
            return OpprettOppgave("Bruker er innvilget overgangsstønad for ufødt barn (${barnFødtFørTermin.separerteIdenter()}). " +
                                  "Barnet er registrert født i måneden før oppgitt termindato. " +
                                  (if (nyttBarn.isNotEmpty()) "Bruker har også fått et nytt/nye barn (${nyttBarn.separerteIdenter()}). " else "") +
                                  "Vurder saken.")
        }
        return OpprettOppgave("Bruker har fått et nytt/nye barn (${nyttBarn.separerteIdenter()}).")
    }

    private fun List<NyttBarn>.separerteIdenter() = this.joinToString(", ") { it.personIdent }
}
