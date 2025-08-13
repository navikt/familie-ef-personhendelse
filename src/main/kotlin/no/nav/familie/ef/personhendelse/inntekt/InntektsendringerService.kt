package no.nav.familie.ef.personhendelse.inntekt

import no.nav.familie.ef.personhendelse.client.ForventetInntektForPerson
import no.nav.familie.ef.personhendelse.client.OppgaveClient
import no.nav.familie.ef.personhendelse.client.SakClient
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.YearMonth
import kotlin.math.abs

@Service
class InntektsendringerService(
    val oppgaveClient: OppgaveClient,
    val sakClient: SakClient,
    val inntektsendringerRepository: InntektsendringerRepository,
    val inntektClient: InntektClient,
    val taskService: TaskService,
) {
    private val grunnbeløp = Grunnbeløp().nyesteGrunnbeløp.grunnbeløp
    private val halvtGrunnbeløpMånedlig = (grunnbeløp / 2.toBigDecimal()) / 12.toBigDecimal()
    private val maksInntekt = Grunnbeløp().maksInntekt

    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    @Async
    fun beregnInntektsendringerAsync() {
        beregnInntektsendringerOgLagreIDb()
    }

    fun hentPersonerMedInntektsendringerOgRevurderAutomatisk() {
        val inntektsendringer = inntektsendringerRepository.hentKandidaterTilAutomatiskRevurdering()
        inntektsendringer.forEach {
            val yearMonthProssesertTid = YearMonth.from(it.prosessertTid)
            val payload = PayloadRevurderAutomatiskPersonerMedInntektsendringerTask(personIdent = it.personIdent, harIngenEksisterendeYtelser = it.harIngenEksisterendeYtelser(), yearMonthProssesertTid = yearMonthProssesertTid)
            val skalOppretteTask = taskService.finnTaskMedPayloadOgType(objectMapper.writeValueAsString(payload), RevurderAutomatiskPersonerMedInntektsendringerTask.TYPE) == null

            if (skalOppretteTask) {
                val task = RevurderAutomatiskPersonerMedInntektsendringerTask.opprettTask(payload)
                taskService.save(task)
            }
        }
    }

    fun beregnInntektsendringerOgLagreIDb() {
        logger.info("Starter beregning av inntektsendringer")

        val personerMedAktivStønad = sakClient.hentPersonerMedAktivStønadIkkeManueltRevurdertSisteMåneder(3)
        inntektsendringerRepository.clearInntektsendringer()

        logger.info("Antall personer med aktiv stønad: ${personerMedAktivStønad.size}")

        var counter = 0

        personerMedAktivStønad.chunked(500).forEach {
            sakClient.hentForventetInntektForIdenter(it).forEach { forventetInntektForPerson ->
                val inntektResponse = hentInntekt(personIdent = forventetInntektForPerson.personIdent)

                if (inntektResponse != null && forventetInntektForPerson.erSiste2MånederNotNull()) {
                    lagreInntektsendringForPerson(
                        forventetInntektForPerson = forventetInntektForPerson,
                        inntektResponse = inntektResponse,
                    )
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
        inntektResponse: InntektResponse,
    ) {
        val nyeVedtak = VedtakendringerUtil.nyeVedtak(inntektResponse)

        val endretInntekt = beregnEndretInntekt(inntektResponse, forventetInntektForPerson)

        inntektsendringerRepository.lagreVedtakOgInntektsendringForPersonIdent(
            personIdent = forventetInntektForPerson.personIdent,
            harNyeVedtak = nyeVedtak.isNotEmpty(),
            nyeYtelser = nyeVedtak.joinToString(),
            inntektsendring = endretInntekt,
            eksisterendeYtelser = VedtakendringerUtil.offentligeYtelserForNyesteMåned(inntektResponse).joinToString(),
        )
    }

    fun hentInntekt(personIdent: String): InntektResponse? {
        try {
            return inntektClient.hentInntekt(
                personIdent = personIdent,
                månedFom = YearMonth.now().minusMonths(5),
                månedTom = YearMonth.now(),
            )
        } catch (e: Exception) {
            secureLogger.warn("Feil ved kall mot inntektskomponenten (inntektV2) ved kall mot person $personIdent. Melding: ${e.message}. Årsak: ${e.cause}.")
        }

        return null
    }

    fun beregnEndretInntekt(
        inntektResponse: InntektResponse,
        forventetInntektForPerson: ForventetInntektForPerson,
    ): Inntektsendring {
        // hent alle registrerte vedtak som var på personen sist beregning
        val nyesteRegistrerteInntekt = inntektResponse.inntektsmåneder.filter { it.måned == YearMonth.now().minusMonths(1) }
        val nestNyesteRegistrerteInntekt = inntektResponse.inntektsmåneder.filter { it.måned == YearMonth.now().minusMonths(2) }
        val inntektTreMånederTilbake = inntektResponse.inntektsmåneder.filter { it.måned == YearMonth.now().minusMonths(3) }
        val inntektFireMånederTilbake = inntektResponse.inntektsmåneder.filter { it.måned == YearMonth.now().minusMonths(4) }

        val inntektsendringFireMånederTilbake =
            beregnInntektsendring(
                nyesteRegistrerteInntekt = inntektFireMånederTilbake,
                ident = forventetInntektForPerson.personIdent,
                forventetInntekt = forventetInntektForPerson.forventetInntektTreMånederTilbake,
            )

        val inntektsendringTreMånederTilbake =
            beregnInntektsendring(
                nyesteRegistrerteInntekt = inntektTreMånederTilbake,
                ident = forventetInntektForPerson.personIdent,
                forventetInntekt = forventetInntektForPerson.forventetInntektTreMånederTilbake,
            )
        val inntektsendringToMånederTilbake =
            beregnInntektsendring(
                nyesteRegistrerteInntekt = nestNyesteRegistrerteInntekt,
                ident = forventetInntektForPerson.personIdent,
                forventetInntekt = forventetInntektForPerson.forventetInntektToMånederTilbake,
            )
        val inntektsendringForrigeMåned =
            beregnInntektsendring(
                nyesteRegistrerteInntekt = nyesteRegistrerteInntekt,
                ident = forventetInntektForPerson.personIdent,
                forventetInntekt = forventetInntektForPerson.forventetInntektForrigeMåned,
            )

        return Inntektsendring(
            fireMånederTilbake = inntektsendringFireMånederTilbake,
            treMånederTilbake = inntektsendringTreMånederTilbake,
            toMånederTilbake = inntektsendringToMånederTilbake,
            forrigeMåned = inntektsendringForrigeMåned,
        )
    }

    private fun beregnInntektsendring(
        nyesteRegistrerteInntekt: List<Inntektsmåned>,
        ident: String,
        forventetInntekt: Int?,
    ): BeregningResultat {
        if (forventetInntekt == null ||
            nyesteRegistrerteInntekt.isEmpty() ||
            nyesteRegistrerteInntekt
                .firstOrNull()
                ?.inntektListe
                .isNullOrEmpty()
        ) {
            secureLogger.warn("Ingen gjeldende inntekt funnet på person $ident har personen løpende stønad?")
            return BeregningResultat(0, 0, 0)
        }

        if (forventetInntekt > maksInntekt) return BeregningResultat(0, 0, 0)
        val månedligForventetInntekt = (forventetInntekt / 12)

        val inntektListe = nyesteRegistrerteInntekt.flatMap { it.inntektListe }
        val samletInntekt = inntektListe.filterNot { ignorerteYtelserOgUtbetalinger.contains(it.beskrivelse) }.sumOf { it.beløp }.toInt()

        if (samletInntekt < halvtGrunnbeløpMånedlig.toInt()) return BeregningResultat(0, 0, 0)

        secureLogger.info(
            "Samlet inntekt: $samletInntekt - månedlig forventet inntekt: $månedligForventetInntekt  (årlig: $forventetInntekt) for person $ident",
        )

        val inntektsendringProsent = (((samletInntekt - månedligForventetInntekt) / månedligForventetInntekt.toDouble()) * 100).toInt()
        val endretInntektBeløp = samletInntekt - månedligForventetInntekt
        val feilutbetaling = beregnFeilutbetalingForMåned(månedligForventetInntekt, samletInntekt)

        if (månedligForventetInntekt == 0) return BeregningResultat(endretInntektBeløp, 100, feilutbetaling) // Prioriterer personer registrert med uredusert stønad, men har samlet inntekt over 1/2 G
        return BeregningResultat(endretInntektBeløp, inntektsendringProsent, feilutbetaling)
    }

    // Ignorterte ytelser: Alle uføre går under annet regelverk (samordning) og skal derfor ignoreres.
    val ignorerteYtelserOgUtbetalinger =
        listOf(
            "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere",
            "ufoeretrygd",
            "ufoereytelseEtteroppgjoer",
            "ufoerepensjonFraAndreEnnFolketrygden",
            "barnepensjon",
            "introduksjonsstoenad",
        )
}

data class Inntektsendring(
    val fireMånederTilbake: BeregningResultat,
    val treMånederTilbake: BeregningResultat,
    val toMånederTilbake: BeregningResultat,
    val forrigeMåned: BeregningResultat,
) {
    fun harEndretInntekt() = treMånederTilbake.prosent >= 10 && toMånederTilbake.prosent >= 10 && forrigeMåned.prosent >= 10
}

data class BeregningResultat(
    val beløp: Int,
    val prosent: Int,
    val feilutbetaling: Int,
)
