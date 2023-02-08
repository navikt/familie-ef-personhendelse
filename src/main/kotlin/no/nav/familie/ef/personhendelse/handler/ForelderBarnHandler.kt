package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.util.identerUtenAktørId
import no.nav.familie.kontrakter.ef.personhendelse.NyeBarnDto
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
    private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    override val type = PersonhendelseType.FORELDERBARNRELASJON

    override fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): OppgaveInformasjon {
        val personIdent = personhendelse.identerUtenAktørId().first()
        val nyeBarnForBruker = sakClient.finnNyeBarnForBruker(PersonIdent(personIdent))
        if (nyeBarnForBruker.nyeBarn.isEmpty() || personhendelse.forelderBarnRelasjon.relatertPersonsRolle != "BARN") {
            return IkkeOpprettOppgave
        }

        logger.info(
            "Hendelse=${personhendelse.hendelseId} - " +
                "brukeren har ${nyeBarnForBruker.nyeBarn.size} nye barn " +
                "årsaker=${nyeBarnForBruker.nyeBarn.map { it.årsak }.toSet()}",
        )
        secureLogger.info("Nye barn for bruker er ${nyeBarnForBruker.nyeBarn}, hendelseId : ${personhendelse.hendelseId}")

        val barnFødtFørTermin = nyeBarnForBruker.filtrerÅrsak(NyttBarnÅrsak.FØDT_FØR_TERMIN)
        val nyeBarnSomIkkeFinnesPåBehandlingen = nyeBarnForBruker.filtrerÅrsak(NyttBarnÅrsak.BARN_FINNES_IKKE_PÅ_BEHANDLING)
        if (barnFødtFørTermin.isNotEmpty()) {
            val nyeBarnTekst = if (nyeBarnSomIkkeFinnesPåBehandlingen.isNotEmpty()) {
                "Bruker har også fått et nytt/nye barn ${nyeBarnSomIkkeFinnesPåBehandlingen.separerteIdenterMedStønadstype()}. "
            } else {
                ""
            }
            return OpprettOppgave(
                "Bruker er innvilget stønad for ufødt(e) barn ${barnFødtFørTermin.separerteIdenterMedStønadstype()}. " +
                    "Barnet er registrert født i måneden før oppgitt termindato. " +
                    nyeBarnTekst +
                    "Vurder saken.",
            )
        }
        return OpprettOppgave("Bruker har fått et nytt/nye barn ${nyeBarnSomIkkeFinnesPåBehandlingen.separerteIdenterMedStønadstype()} som ikke finnes på behandling.")
    }

    private fun NyeBarnDto.filtrerÅrsak(årsak: NyttBarnÅrsak) = this.nyeBarn.filter { it.årsak == årsak }

    private fun List<NyttBarn>.separerteIdenterMedStønadstype() = this.joinToString(", ") { it.personIdent + " (${it.stønadstype.name.enumToReadable()})" }
}
