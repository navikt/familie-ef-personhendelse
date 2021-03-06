package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.client.ArbeidsfordelingClient
import no.nav.familie.ef.personhendelse.client.ForventetInntektForPerson
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.fristFerdigstillelse
import no.nav.familie.ef.personhendelse.handler.PersonhendelseService
import no.nav.familie.ef.personhendelse.inntekt.vedtak.EfVedtakRepository
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Month
import java.time.YearMonth

@Service
class VedtakendringerService(
    val efVedtakRepository: EfVedtakRepository,
    val inntektClient: InntektClient,
    val oppgaveClient: OppgaveClient,
    val sakClient: SakClient,
    val arbeidsfordelingClient: ArbeidsfordelingClient,
    val inntektsendringerService: InntektsendringerService,
    val personhendelseService: PersonhendelseService
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    @Async
    fun beregnInntektsendringerOgLagreIDb(skalOppretteOppgave: Boolean = false) {

        logger.info("Starter beregning av inntektsendringer")
        val personerMedAktivStønad = sakClient.hentPersonerMedAktivStønad()
        efVedtakRepository.clearInntektsendringer()
        logger.info("Antall personer med aktiv stønad: ${personerMedAktivStønad.size}")
        var counter = 0
        personerMedAktivStønad.chunked(500).forEach {
            sakClient.hentForventetInntektForIdenter(it).forEach { forventetInntektForPerson ->
                val response = hentInntektshistorikk(forventetInntektForPerson.personIdent)
                if (response != null &&
                    forventetInntektForPerson.forventetInntektForrigeMåned != null &&
                    forventetInntektForPerson.forventetInntektToMånederTilbake != null
                ) {
                    lagreInntektsendringForPerson(forventetInntektForPerson, response)
                }
                counter++
                if (counter % 500 == 0) {
                    logger.info("Antall personer sjekket: $counter (av ${personerMedAktivStønad.size}")
                }
            }
        }

        logger.info("Vedtak- og inntektsendringer ferdig")
    }

    private fun lagreInntektsendringForPerson(
        forventetInntektForPerson: ForventetInntektForPerson,
        response: InntektshistorikkResponse
    ) {
        val harNyeVedtak = harNyeVedtak(response)
        val harEndretInntekt = inntektsendringerService.harEndretInntekt(response, forventetInntektForPerson)
        efVedtakRepository.lagreInntektsendring(forventetInntektForPerson.personIdent, harNyeVedtak, harEndretInntekt)
    }

    fun opprettOppgaverForInntektsendringer(skalOppretteOppgave: Boolean): Int {
        val inntektsendringer = efVedtakRepository.hentInntektsendringer()
        if (skalOppretteOppgave) {
            inntektsendringer.forEach {
                opprettOppgave(it.harNyttVedtak, it.harEndretInntekt, it.personIdent)
            }
        } else {
            logger.info("Ville opprettet inntektsendring-oppgave for ${inntektsendringer.size} personer")
        }
        return inntektsendringer.size
    }

    fun harNyeVedtak(inntektshistorikkResponse: InntektshistorikkResponse): Boolean {
        // hent alle registrerte vedtak som var på personen sist beregning
        val nyesteRegistrerteInntekt =
            inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(1).toString())
        val nestNyesteRegistrerteInntekt =
            inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(2).toString())

        val antallOffentligeYtelserForNyeste = antallOffentligeYtelser(nyesteRegistrerteInntekt)
        val antallOffentligeYtelserForNestNyeste = antallOffentligeYtelser(nestNyesteRegistrerteInntekt)

        return antallOffentligeYtelserForNyeste > antallOffentligeYtelserForNestNyeste
    }

    private fun antallOffentligeYtelser(nyesteRegistrerteInntekt: List<InntektVersjon>?): Int {
        val offentligYtelseInntekt = nyesteRegistrerteInntekt?.filter {
            it.arbeidsInntektInformasjon.inntektListe?.any {
                it.inntektType == InntektType.YTELSE_FRA_OFFENTLIGE &&
                    it.beskrivelse != "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere"
            }
                ?: false
        }

        val nyesteVersjon = offentligYtelseInntekt?.maxOfOrNull { it.versjon }

        val inntektListe =
            offentligYtelseInntekt?.firstOrNull { it.versjon == nyesteVersjon }?.arbeidsInntektInformasjon?.inntektListe
        return inntektListe?.filter {
            it.inntektType == InntektType.YTELSE_FRA_OFFENTLIGE &&
                it.beskrivelse != "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere" &&
                it.tilleggsinformasjon?.tilleggsinformasjonDetaljer?.detaljerType != "ETTERBETALINGSPERIODE"
        }?.groupBy { it.beskrivelse }?.size ?: 0
    }

    private fun hentInntektshistorikk(fnr: String): InntektshistorikkResponse? {
        try {
            return inntektClient.hentInntektshistorikk(
                fnr,
                YearMonth.now().minusMonths(4),
                null
            )
        } catch (e: Exception) {
            secureLogger.warn("Feil ved kall mot inntektskomponenten ved kall mot person $fnr. Message: ${e.message} Cause: ${e.cause}")
        }
        return null
    }

    private fun opprettOppgave(
        harNyeVedtak: Boolean,
        harEndretInntekt: Boolean,
        personident: String,
        stønadType: StønadType = StønadType.OVERGANGSSTØNAD
    ) {
        val oppgavetekst = lagOppgavetekst(harNyeVedtak, harEndretInntekt)
        secureLogger.info("$personident - $oppgavetekst")
        val enhetsnummer =
            arbeidsfordelingClient.hentArbeidsfordelingEnhetId(personident)
        val oppgaveId = oppgaveClient.opprettOppgave(
            OpprettOppgaveRequest(
                ident = OppgaveIdentV2(
                    ident = personident,
                    gruppe = IdentGruppe.FOLKEREGISTERIDENT
                ),
                saksId = null,
                tema = Tema.ENF,
                oppgavetype = Oppgavetype.VurderKonsekvensForYtelse,
                fristFerdigstillelse = fristFerdigstillelse(),
                beskrivelse = oppgavetekst,
                enhetsnummer = enhetsnummer,
                behandlingstema = stønadType.tilBehandlingstemaValue(),
                tilordnetRessurs = null,
                behandlesAvApplikasjon = "familie-ef-sak"
            )
        )
        secureLogger.info("Opprettet oppgave for person $personident med id: $oppgaveId")
        try {
            personhendelseService.leggOppgaveIMappe(oppgaveId)
        } catch (e: Exception) {
            logger.error("Feil under knytning av mappe til oppgave - se securelogs for stacktrace")
            secureLogger.error("Feil under knytning av mappe til oppgave", e)
        }
    }

    private fun lagOppgavetekst(harNyeVedtak: Boolean, harEndretInntekt: Boolean): String {
        val måned = YearMonth.now().minusMonths(1).month.tilNorsk()
        if (harNyeVedtak && harEndretInntekt) {
            return "Person har fått utbetalt ny stønad fra NAV og har økt inntekt i $måned. Vurder om overgangsstønaden skal revurderes."
        } else if (harNyeVedtak) {
            return "Person har fått utbetalt ny stønad fra NAV i $måned. Vurder om overgangsstønaden skal revurderes."
        } else {
            return "Person har økt inntekt i $måned. Vurder om overgangsstønaden skal revurderes."
        }
    }
}

fun StønadType.tilBehandlingstemaValue(): String {
    return when (this) {
        StønadType.OVERGANGSSTØNAD -> Behandlingstema.Overgangsstønad.value
        StønadType.BARNETILSYN -> Behandlingstema.Barnetilsyn.value
        StønadType.SKOLEPENGER -> Behandlingstema.Skolepenger.value
    }
}

fun Month.tilNorsk(): String =
    when (this.name) {
        Month.JANUARY.name -> "januar"
        Month.FEBRUARY.name -> "februar"
        Month.MARCH.name -> "mars"
        Month.APRIL.name -> "april"
        Month.MAY.name -> "mai"
        Month.JUNE.name -> "juni"
        Month.JULY.name -> "juli"
        Month.AUGUST.name -> "august"
        Month.SEPTEMBER.name -> "september"
        Month.OCTOBER.name -> "oktober"
        Month.NOVEMBER.name -> "november"
        Month.DECEMBER.name -> "desember"
        else -> this.name
    }
