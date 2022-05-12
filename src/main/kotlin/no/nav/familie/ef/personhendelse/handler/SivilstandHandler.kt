package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.datoutil.tilNorskDatoformat
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class SivilstandHandler : PersonhendelseHandler {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override val type = PersonhendelseType.SIVILSTAND

    override fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): OppgaveInformasjon {
        val sivilstand = personhendelse.sivilstand
        val gyldigFraOgMed = sivilstand.gyldigFraOgMed ?: sivilstand.bekreftelsesdato
        if (personhendelse.sivilstandNotNull()) {
            logger.info("Mottatt sivilstand hendelse med verdi ${sivilstand.type} gyldigFraOgMed=${gyldigFraOgMed}")
        }
        if (!personhendelse.skalSivilstandHåndteres() || erEldreEnn2År(gyldigFraOgMed)) {
            return IkkeOpprettOppgave
        }

        val beskrivelse = "Sivilstand endret til \"${sivilstand.type.enumToReadable()}\", " +
                          "gyldig fra og med dato: ${gyldigFraOgMed.tilNorskDatoformat()}"
        return OpprettOppgave(beskrivelse = beskrivelse)
    }

    private fun erEldreEnn2År(gyldigFraOgMed: LocalDate?) =
            gyldigFraOgMed != null && gyldigFraOgMed < LocalDate.now().minusYears(2)
}

fun Personhendelse.skalSivilstandHåndteres(): Boolean {
    return this.sivilstandNotNull() &&
        (sivilstandTyperSomSkalHåndteres.contains(this.sivilstand.type)) &&
        (endringstyperSomSkalHåndteres.contains(this.endringstype))
}

private fun Personhendelse.sivilstandNotNull() = this.sivilstand != null && this.sivilstand.type != null

private val sivilstandTyperSomSkalHåndteres = listOf("GIFT", "REGISTRERT_PARTNER")

private val endringstyperSomSkalHåndteres = listOf(Endringstype.OPPRETTET, Endringstype.KORRIGERT)

fun String.enumToReadable(): String {
    return this.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}
