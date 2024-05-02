package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.client.ArbeidsfordelingClient
import no.nav.familie.ef.personhendelse.client.ForventetInntektForPerson
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.ef.personhendelse.client.fristFerdigstillelse
import no.nav.familie.ef.personhendelse.inntekt.vedtak.EfVedtakRepository
import no.nav.familie.ef.personhendelse.inntekt.vedtak.InntektOgVedtakEndring
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Service
class VedtakendringerService(
    val efVedtakRepository: EfVedtakRepository,
    val inntektClient: InntektClient,
    val oppgaveClient: OppgaveClient,
    val sakClient: SakClient,
    val arbeidsfordelingClient: ArbeidsfordelingClient,
    val inntektsendringerService: InntektsendringerService,
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    fun beregnInntektsendringerOgLagreIDb() {
        logger.info("Starter beregning av inntektsendringer")
        val personerMedAktivStønad = sakClient.hentPersonerMedAktivStønadIkkeManueltRevurdertSisteMåneder(3)
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

    fun harNyeVedtak(response: InntektshistorikkResponse) = nyeVedtak(response)?.isNotEmpty() ?: false

    private fun lagreInntektsendringForPerson(
        forventetInntektForPerson: ForventetInntektForPerson,
        response: InntektshistorikkResponse,
    ) {
        val nyeVedtak = nyeVedtak(response)
        val endretInntekt = inntektsendringerService.beregnEndretInntekt(response, forventetInntektForPerson)
        efVedtakRepository.lagreVedtakOgInntektsendringForPersonIdent(
            forventetInntektForPerson.personIdent,
            nyeVedtak?.isNotEmpty() ?: false,
            nyeVedtak?.joinToString(),
            endretInntekt,
        )
    }

    fun opprettOppgaverForInntektsendringer(skalOppretteOppgave: Boolean): Int {
        val inntektsendringer = efVedtakRepository.hentInntektsendringerSomSkalHaOppgave()
        if (skalOppretteOppgave) {
            inntektsendringer.forEach {
                opprettOppgaveForInntektsendring(it, lagOppgavetekstForInntektsendring(it))
            }
        } else {
            logger.info("Ville opprettet inntektsendring-oppgave for ${inntektsendringer.size} personer")
        }
        return inntektsendringer.size
    }

    fun opprettOppgaverForNyeVedtakUføretrygd() {
        val nyeUføretrygdVedtak = efVedtakRepository.hentInntektsendringerForUføretrygd()
        nyeUføretrygdVedtak.forEach {
            opprettOppgaveForInntektsendring(it, lagOppgavetekstVedNyYtelseUføretrygd())
        }
    }

    fun nyeVedtak(inntektshistorikkResponse: InntektshistorikkResponse): List<String>? {
        // hent alle registrerte vedtak som var på personen sist beregning
        val nyesteRegistrerteInntekt =
            inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(1))
        val nestNyesteRegistrerteInntekt =
            inntektshistorikkResponse.inntektForMåned(YearMonth.now().minusMonths(2))

        val offentligeYtelserForNyesteMåned = offentligeYtelser(nyesteRegistrerteInntekt) ?: emptyList()
        val offentligeYtelserForNestNyesteMåned = offentligeYtelser(nestNyesteRegistrerteInntekt) ?: emptyList()

        return offentligeYtelserForNyesteMåned.minus(offentligeYtelserForNestNyesteMåned)
    }

    private fun offentligeYtelser(nyesteRegistrerteInntekt: List<InntektVersjon>?): List<String>? {
        val offentligYtelseInntekt = nyesteRegistrerteInntekt?.filter {
            it.arbeidsInntektInformasjon.inntektListe?.any { offentligYtelse ->
                offentligYtelse.inntektType == InntektType.YTELSE_FRA_OFFENTLIGE &&
                    offentligYtelse.beskrivelse != "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere"
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
        }?.groupBy { it.beskrivelse }?.map { it.key }
    }

    private fun hentInntektshistorikk(fnr: String): InntektshistorikkResponse? {
        try {
            return inntektClient.hentInntektshistorikk(
                fnr,
                YearMonth.now().minusMonths(5),
                null,
            )
        } catch (e: Exception) {
            secureLogger.warn("Feil ved kall mot inntektskomponenten ved kall mot person $fnr. Message: ${e.message} Cause: ${e.cause}")
        }
        return null
    }

    private fun opprettOppgaveForInntektsendring(
        inntektOgVedtakEndring: InntektOgVedtakEndring,
        beskrivelse: String,
    ) {
        // val oppgavetekst = lagOppgavetekst(harNyeVedtak, inntektsendringToMånederInntekt >= 10 && inntektsendringForrigeMåned >= 10)
        val oppgaveId = oppgaveClient.opprettOppgave(
            OpprettOppgaveRequest(
                ident = OppgaveIdentV2(
                    ident = inntektOgVedtakEndring.personIdent,
                    gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                ),
                saksId = null,
                tema = Tema.ENF,
                oppgavetype = Oppgavetype.VurderInntekt,
                fristFerdigstillelse = fristFerdigstillelse(),
                beskrivelse = beskrivelse,
                enhetsnummer = arbeidsfordelingClient.hentArbeidsfordelingEnhetId(inntektOgVedtakEndring.personIdent),
                behandlingstema = null, // Gjelder-feltet i Gosys
                tilordnetRessurs = null,
                behandlesAvApplikasjon = null,
            ),
        )
        secureLogger.info("Opprettet oppgave for person ${inntektOgVedtakEndring.personIdent} med id: $oppgaveId")
        oppgaveClient.leggOppgaveIMappe(oppgaveId, "63") // Inntektskontroll
    }

    fun lagOppgavetekstForInntektsendring(inntektOgVedtakEndring: InntektOgVedtakEndring): String {
        val totalFeilutbetaling = inntektOgVedtakEndring.inntektsendringFireMånederTilbake.feilutbetaling +
            inntektOgVedtakEndring.inntektsendringTreMånederTilbake.feilutbetaling +
            inntektOgVedtakEndring.inntektsendringToMånederTilbake.feilutbetaling +
            inntektOgVedtakEndring.inntektsendringForrigeMåned.feilutbetaling

        val årMånedProsessert = YearMonth.from(inntektOgVedtakEndring.prosessertTid)

        val periodeTekst =
            "FOM ${årMånedProsessert.minusMonths(4).norskFormat()} - TOM ${årMånedProsessert.minusMonths(1).norskFormat()}"
        val oppgavetekst = "Uttrekksperiode: $periodeTekst \n" +
            "Beregnet feilutbetaling i uttrekksperioden: ${totalFeilutbetaling.tusenskille()} kroner "
        return oppgavetekst
    }

    fun lagOppgavetekstVedNyYtelseUføretrygd(): String {
        return "Bruker har fått utbetalt uføretrygd. Vurder samordning."
    }

    private fun YearMonth.norskFormat() = this.format(DateTimeFormatter.ofPattern("MM.yyyy"))
    private fun Int.tusenskille() = String.format("%,d", this).replace(",", " ")
    private fun lagOppgavetekst(harNyeVedtak: Boolean, harEndretInntekt: Boolean): String {
        val forrigeMåned = YearMonth.now().minusMonths(1).month.tilNorsk()
        val toMånederTilbake = YearMonth.now().minusMonths(2).month.tilNorsk()

        if (harNyeVedtak && harEndretInntekt) {
            return "Person har fått utbetalt ny stønad fra NAV og har økt inntekt i $toMånederTilbake og $forrigeMåned. Vurder om overgangsstønaden skal revurderes."
        } else if (harNyeVedtak) {
            return "Person har fått utbetalt ny stønad fra NAV i $forrigeMåned. Vurder om overgangsstønaden skal revurderes."
        } else {
            return "Person har økt inntekt i $toMånederTilbake og $forrigeMåned. Vurder om overgangsstønaden skal revurderes."
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
