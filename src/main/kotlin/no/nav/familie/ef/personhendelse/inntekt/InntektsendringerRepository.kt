package no.nav.familie.ef.personhendelse.inntekt

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID

@Repository
class InntektsendringerRepository(
    val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun lagreVedtakOgInntektsendringForPersonIdent(
        personIdent: String,
        harNyeVedtak: Boolean,
        nyeYtelser: String?,
        inntektsendring: Inntektsendring,
        eksisterendeYtelser: String?,
    ) {
        val sql =
            "INSERT INTO inntektsendringer(" +
                "id, person_ident, harnyttvedtak, ny_ytelse_type, prosessert_tid, " +
                "inntekt_endret_fire_maaneder_tilbake, inntekt_endret_tre_maaneder_tilbake, inntekt_endret_to_maaneder_tilbake, inntekt_endret_forrige_maaned, " +
                "inntekt_endret_fire_maaneder_tilbake_belop, inntekt_endret_tre_maaneder_tilbake_belop, inntekt_endret_to_maaneder_tilbake_belop, inntekt_endret_forrige_maaned_belop," +
                "feilutbetaling_fire_maaneder_tilbake, feilutbetaling_tre_maaneder_tilbake, feilutbetaling_to_maaneder_tilbake, feilutbetaling_forrige_maaned," +
                "eksisterende_ytelser" +
                ") VALUES" +
                "(:id, :personIdent, :harNyeVedtak, :nyeYtelser, :prosessertTid, " +
                ":inntektsendringFireMånederTilbake, :inntektsendringTreMånederTilbake, :inntektsendringToMånederTilbake, :inntektsendringForrigeMåned, " +
                ":inntektsendringFireMånederTilbakeBeløp, :inntektsendringTreMånederTilbakeBeløp, :inntektsendringToMånederTilbakeBeløp, :inntektsendringForrigeMånedBeløp, " +
                ":feilutbetalingFireMånederTilbake, :feilutbetalingTreMånederTilbake, :feilutbetalingToMånederTilbake, :feilutbetalingForrigeMåned, :eksisterendeYtelser) " +
                "ON CONFLICT DO NOTHING"
        val params =
            MapSqlParameterSource(
                mapOf(
                    "id" to UUID.randomUUID(),
                    "personIdent" to personIdent,
                    "harNyeVedtak" to harNyeVedtak,
                    "nyeYtelser" to nyeYtelser,
                    "prosessertTid" to LocalDateTime.now(),
                    "inntektsendringFireMånederTilbake" to inntektsendring.fireMånederTilbake.prosent,
                    "inntektsendringTreMånederTilbake" to inntektsendring.treMånederTilbake.prosent,
                    "inntektsendringToMånederTilbake" to inntektsendring.toMånederTilbake.prosent,
                    "inntektsendringForrigeMåned" to inntektsendring.forrigeMåned.prosent,
                    "inntektsendringFireMånederTilbakeBeløp" to inntektsendring.fireMånederTilbake.beløp,
                    "inntektsendringTreMånederTilbakeBeløp" to inntektsendring.treMånederTilbake.beløp,
                    "inntektsendringToMånederTilbakeBeløp" to inntektsendring.toMånederTilbake.beløp,
                    "inntektsendringForrigeMånedBeløp" to inntektsendring.forrigeMåned.beløp,
                    "feilutbetalingFireMånederTilbake" to inntektsendring.fireMånederTilbake.feilutbetaling,
                    "feilutbetalingTreMånederTilbake" to inntektsendring.treMånederTilbake.feilutbetaling,
                    "feilutbetalingToMånederTilbake" to inntektsendring.toMånederTilbake.feilutbetaling,
                    "feilutbetalingForrigeMåned" to inntektsendring.forrigeMåned.feilutbetaling,
                    "eksisterendeYtelser" to eksisterendeYtelser,
                ),
            )
        namedParameterJdbcTemplate.update(sql, params)
    }

    fun hentInntektsendringerSomSkalHaOppgave(): List<InntektOgVedtakEndring> {
        val sql =
            "SELECT * FROM inntektsendringer WHERE " +
                "(inntekt_endret_tre_maaneder_tilbake >= 10 AND " +
                "inntekt_endret_to_maaneder_tilbake >= 10 AND " +
                "inntekt_endret_forrige_maaned >= 10) AND " +
                "(feilutbetaling_tre_maaneder_tilbake + feilutbetaling_to_maaneder_tilbake + feilutbetaling_forrige_maaned) > 30000"
        return namedParameterJdbcTemplate.query(sql, inntektsendringerMapper)
    }

    fun hentInntektsendringerForUføretrygd(): List<InntektOgVedtakEndring> {
        val sql = "SELECT * FROM inntektsendringer WHERE harnyttvedtak is TRUE AND ny_ytelse_type like '%ufoeretrygd%'"
        return namedParameterJdbcTemplate.query(sql, inntektsendringerMapper)
    }

    fun hentInntektsendringerForUføretrygdSomHarUføretrygd(): List<InntektOgVedtakEndring> {
        val sql = "SELECT * FROM inntektsendringer WHERE eksisterende_ytelser like '%ufoeretrygd%'"
        return namedParameterJdbcTemplate.query(sql, inntektsendringerMapper)
    }

    fun hentKandidaterTilAutomatiskRevurdering(): List<InntektOgVedtakEndring> {
        val sql =
            "SELECT * FROM inntektsendringer WHERE " +
                "(inntekt_endret_tre_maaneder_tilbake >= 10 AND " +
                "inntekt_endret_to_maaneder_tilbake >= 10 AND " +
                "inntekt_endret_forrige_maaned >= 10) AND " +
                "(feilutbetaling_tre_maaneder_tilbake + feilutbetaling_to_maaneder_tilbake + feilutbetaling_forrige_maaned) < 30000 AND " +
                "(feilutbetaling_tre_maaneder_tilbake + feilutbetaling_to_maaneder_tilbake + feilutbetaling_forrige_maaned) > 5000"
        return namedParameterJdbcTemplate.query(sql, inntektsendringerMapper)
    }

    fun hentInntektsendringerForPersonMedArbeidsavklaringspenger(): List<InntektOgVedtakEndring> {
        val sql =
            "SELECT * FROM inntektsendringer WHERE eksisterende_ytelser like '%arbeidsavklaringspenger%'"
        return namedParameterJdbcTemplate.query(sql, inntektsendringerMapper)
    }

    private val inntektsendringerMapper = { rs: ResultSet, _: Int ->
        InntektOgVedtakEndring(
            rs.getString("person_ident"),
            rs.getBoolean("harNyttVedtak"),
            rs.getObject("prosessert_tid", LocalDateTime::class.java),
            BeregningResultat(
                rs.getInt("inntekt_endret_fire_maaneder_tilbake_belop"),
                rs.getInt("inntekt_endret_fire_maaneder_tilbake"),
                rs.getInt("feilutbetaling_fire_maaneder_tilbake"),
            ),
            BeregningResultat(
                rs.getInt("inntekt_endret_tre_maaneder_tilbake_belop"),
                rs.getInt("inntekt_endret_tre_maaneder_tilbake"),
                rs.getInt("feilutbetaling_tre_maaneder_tilbake"),
            ),
            BeregningResultat(
                rs.getInt("inntekt_endret_to_maaneder_tilbake_belop"),
                rs.getInt("inntekt_endret_to_maaneder_tilbake"),
                rs.getInt("feilutbetaling_to_maaneder_tilbake"),
            ),
            BeregningResultat(
                rs.getInt("inntekt_endret_forrige_maaned_belop"),
                rs.getInt("inntekt_endret_forrige_maaned"),
                rs.getInt("feilutbetaling_forrige_maaned"),
            ),
            rs.getString("ny_ytelse_type"),
            rs.getString("eksisterende_ytelser"),
        )
    }

    fun clearInntektsendringer() {
        val sql = "TRUNCATE TABLE inntektsendringer"
        namedParameterJdbcTemplate.update(sql, MapSqlParameterSource())
    }
}

data class InntektOgVedtakEndring(
    val personIdent: String,
    val harNyeVedtak: Boolean,
    val prosessertTid: LocalDateTime,
    val inntektsendringFireMånederTilbake: BeregningResultat,
    val inntektsendringTreMånederTilbake: BeregningResultat,
    val inntektsendringToMånederTilbake: BeregningResultat,
    val inntektsendringForrigeMåned: BeregningResultat,
    val nyeYtelser: String?,
    val eksisterendeYtelser: String?,
) {
    fun harIngenEksisterendeYtelser(): Boolean {
        val feriePengerYtelser = setOf("feriepengerPleiepenger", "feriepengerForeldrepenger", "feriepengerSykepenger")
        if (eksisterendeYtelser.isNullOrEmpty()) return true
        val list = eksisterendeYtelser.split(",").map { it.trim() }
        return list.size == 1 && list[0] in feriePengerYtelser
    }
}
