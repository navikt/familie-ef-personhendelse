package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.pdl.secureLogger
import no.nav.familie.ef.personhendelse.datoutil.tilNorskDatoformat
import no.nav.familie.ef.personhendelse.generated.enums.Sivilstandstype
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SivilstandHandler : PersonhendelseHandler {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override val type = PersonhendelseType.SIVILSTAND

    override fun lagOppgaveInformasjon(personhendelse: Personhendelse): OppgaveInformasjon {
        val sivilstand = personhendelse.sivilstand
        val gyldigFraOgMed = sivilstand.gyldigFraOgMed ?: sivilstand.bekreftelsesdato
        if (personhendelse.sivilstandNotNull()) {
            logger.info("Mottatt sivilstand hendelse med verdi ${sivilstand.type} gyldigFraOgMed=$gyldigFraOgMed")
        } else {
            if (personhendelse.endringstype == Endringstype.OPPHOERT) {
                secureLogger.error("Opphør av sivilstand uten type. HendelseId: ${personhendelse.hendelseId} tidligere hendelseId: ${personhendelse.tidligereHendelseId}")
            }
            return IkkeOpprettOppgave
        }

        if (opprettetSkiltEllerSeparert(personhendelse)) {
            return IkkeOpprettOppgave
        }

        val beskrivelse = personhendelse.endringstype.tilTekst() + " sivilstand av type \"${sivilstand.type.enumToReadable()}\", " +
            "gyldig fra og med dato: ${gyldigFraOgMed.tilNorskDatoformat()}"
        return OpprettOppgave(beskrivelse = beskrivelse)
    }

    private fun opprettetSkiltEllerSeparert(personhendelse: Personhendelse) =
        (personhendelse.sivilstand.type == Sivilstandstype.SKILT.name || personhendelse.sivilstand.type == Sivilstandstype.SEPARERT.name) &&
            personhendelse.endringstype == Endringstype.OPPRETTET
}
private fun Personhendelse.sivilstandNotNull() = this.sivilstand != null && this.sivilstand.type != null

fun Endringstype.tilTekst(): String {
    return when (this) {
        Endringstype.OPPRETTET -> "Ny"
        Endringstype.KORRIGERT -> "Korrigering av"
        Endringstype.ANNULLERT -> "Annullering av"
        Endringstype.OPPHOERT -> "Opphør av"
    }
}

fun String.enumToReadable(): String {
    return this.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}
