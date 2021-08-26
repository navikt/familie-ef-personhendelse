package no.nav.familie.ef.personhendelse.handler

import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.PdlClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.defaultOpprettOppgaveRequest
import no.nav.familie.ef.personhendelse.generated.enums.ForelderBarnRelasjonRolle
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class SivilstandHandler(
    val sakClient: SakClient,
    val oppgaveClient: OppgaveClient
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun handleSivilstand(personhendelse: Personhendelse) {

        if (personhendelse.sivilstandNotNull()) logger.info("Mottatt sivilstand hendelse med verdi ${personhendelse.sivilstand.type}")
        if (!personhendelse.skalSivilstandHåndteres()) {
            return
        }

        val personIdent = personhendelse.personidenter.map { it.toString() }.first()
        val finnesBehandlingForPerson = sakClient.finnesBehandlingForPerson(personIdent, StønadType.OVERGANGSSTØNAD)
        if (finnesBehandlingForPerson) {
            secureLogger.info("Finnes behandling med personIdent: $personIdent : $finnesBehandlingForPerson")
            val beskrivelse = "Personhendelse: ${personhendelse.sivilstand.type.toString().enumToReadable()} " +
                    "gyldig fra og med ${personhendelse.sivilstand.gyldigFraOgMed.toReadable()}"
            val request = defaultOpprettOppgaveRequest(personIdent, beskrivelse)
            val oppgaveId = oppgaveClient.opprettOppgave(request)
            secureLogger.info("Oppgave opprettet med oppgaveId: $oppgaveId")
        }
    }
}

fun Personhendelse.skalSivilstandHåndteres(): Boolean {
    return this.sivilstandNotNull() ||
            (sivilstandTyperSomSkalHåndteres.contains(this.sivilstand.type.toString()))
            && (endringstyperSomSkalHåndteres.contains(this.endringstype))
}

private fun Personhendelse.sivilstandNotNull() = this.sivilstand != null && this.sivilstand.type != null

private val sivilstandTyperSomSkalHåndteres = listOf("GIFT", "REGISTRERT_PARTNER")

private val endringstyperSomSkalHåndteres = listOf(Endringstype.OPPRETTET, Endringstype.KORRIGERT)

fun String.enumToReadable(): String {
    return this.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

fun LocalDate.toReadable(): String {
    return this.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
}