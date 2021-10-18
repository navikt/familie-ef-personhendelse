package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.datoutil.tilNorskDatoformat
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SivilstandHandler: PersonhendelseHandler {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override val type = PersonhendelseType.SIVILSTAND

    override fun skalOppretteOppgave(personhendelse: Personhendelse): Boolean {
        if (personhendelse.sivilstandNotNull()) {
            logger.info("Mottatt sivilstand hendelse med verdi ${personhendelse.sivilstand.type}")
        }
        return personhendelse.skalSivilstandHåndteres()
    }

    override fun lagOppgaveBeskrivelse(personhendelse: Personhendelse): String {
        return "Sivilstand endret til \"${personhendelse.sivilstand.type.enumToReadable()}\", " +
               "gyldig fra og med dato: ${(personhendelse.sivilstand.bekreftelsesdato ?: personhendelse.sivilstand.gyldigFraOgMed).tilNorskDatoformat()}"
    }

}

fun Personhendelse.skalSivilstandHåndteres(): Boolean {
    return this.sivilstandNotNull() &&
           (sivilstandTyperSomSkalHåndteres.contains(this.sivilstand.type))
           && (endringstyperSomSkalHåndteres.contains(this.endringstype))
}

private fun Personhendelse.sivilstandNotNull() = this.sivilstand != null && this.sivilstand.type != null

private val sivilstandTyperSomSkalHåndteres = listOf("GIFT", "REGISTRERT_PARTNER")

private val endringstyperSomSkalHåndteres = listOf(Endringstype.OPPRETTET, Endringstype.KORRIGERT)

fun String.enumToReadable(): String {
    return this.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}