package no.nav.familie.ef.personhendelse.inntekt.vedtak

import no.nav.familie.ef.personhendelse.inntekt.BeregningResultat
import no.nav.familie.ef.personhendelse.inntekt.Inntektsendring
import no.nav.familie.kontrakter.felles.ef.EnsligForsørgerVedtakhendelse
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@Repository
class EfVedtakRepository(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

    fun lagreEfVedtakshendelse(
        vedtakshendelse: EnsligForsørgerVedtakhendelse,
        aarMaanedProsessert: YearMonth = YearMonth.now(),
    ) {
        val sql =
            "INSERT INTO efvedtakhendelse VALUES(:behandlingId, :personIdent, :stønadType, :aar_maaned_prosessert, :versjon)" +
                " ON CONFLICT DO NOTHING"
        val params = MapSqlParameterSource(
            mapOf(
                "behandlingId" to vedtakshendelse.behandlingId,
                "personIdent" to vedtakshendelse.personIdent,
                "stønadType" to vedtakshendelse.stønadType.toString(),
                "aar_maaned_prosessert" to aarMaanedProsessert.toString(),
                "versjon" to 1,
            ),
        )
        namedParameterJdbcTemplate.update(sql, params)
    }

    fun hentAllePersonerMedVedtak(): List<VedtakhendelseInntektberegning> {
        val sql =
            "SELECT MAX(behandling_id) as behandling_id, person_ident, stonadstype, aar_maaned_prosessert, versjon FROM efvedtakhendelse GROUP BY person_ident, stonadstype, aar_maaned_prosessert, versjon ORDER BY behandling_id"
        val mapSqlParameterSource = MapSqlParameterSource("stonadstype", StønadType.OVERGANGSSTØNAD.toString())
        return namedParameterJdbcTemplate.query(sql, mapSqlParameterSource, vedtakhendelseInntektberegningMapper)
    }

    fun hentPersonerMedVedtakIkkeBehandlet(): List<VedtakhendelseInntektberegning> {
        val sql = "SELECT * FROM efvedtakhendelse WHERE aar_maaned_prosessert != '${YearMonth.now()}'"
        return namedParameterJdbcTemplate.query(sql, vedtakhendelseInntektberegningMapper)
    }

    private val vedtakhendelseInntektberegningMapper = { rs: ResultSet, _: Int ->
        VedtakhendelseInntektberegning(
            rs.getLong("behandling_id"),
            rs.getString("person_ident"),
            StønadType.valueOf(rs.getString("stonadstype")),
            YearMonth.parse(rs.getString("aar_maaned_prosessert")),
            rs.getInt("versjon"),
        )
    }

    fun oppdaterAarMaanedProsessert(personIdent: String, yearMonth: YearMonth = YearMonth.now()) {
        val sql = "UPDATE efvedtakhendelse SET aar_maaned_prosessert = :yearMonth " +
            "WHERE person_ident = :personIdent"
        val mapSqlParameterSource = MapSqlParameterSource("personIdent", personIdent)
            .addValue("yearMonth", yearMonth.toString())

        namedParameterJdbcTemplate.update(sql, mapSqlParameterSource)
    }

    fun lagreVedtakOgInntektsendringForPersonIdent(
        personIdent: String,
        harNyeVedtak: Boolean,
        nyeYtelser: String?,
        inntektsendring: Inntektsendring,
    ) {
        val sql =
            "INSERT INTO inntektsendringer(" +
                "id, person_ident, harnyttvedtak, ny_ytelse_type, prosessert_tid, " +
                "inntekt_endret_fire_maaneder_tilbake, inntekt_endret_tre_maaneder_tilbake, inntekt_endret_to_maaneder_tilbake, inntekt_endret_forrige_maaned, " +
                "inntekt_endret_fire_maaneder_tilbake_belop, inntekt_endret_tre_maaneder_tilbake_belop, inntekt_endret_to_maaneder_tilbake_belop, inntekt_endret_forrige_maaned_belop," +
                "feilutbetaling_fire_maaneder_tilbake, feilutbetaling_tre_maaneder_tilbake, feilutbetaling_to_maaneder_tilbake, feilutbetaling_forrige_maaned" +
                ") VALUES" +
                "(:id, :personIdent, :harNyeVedtak, :nyeYtelser, :prosessertTid, " +
                ":inntektsendringFireMånederTilbake, :inntektsendringTreMånederTilbake, :inntektsendringToMånederTilbake, :inntektsendringForrigeMåned, " +
                ":inntektsendringFireMånederTilbakeBeløp, :inntektsendringTreMånederTilbakeBeløp, :inntektsendringToMånederTilbakeBeløp, :inntektsendringForrigeMånedBeløp, " +
                ":feilutbetalingFireMånederTilbake, :feilutbetalingTreMånederTilbake, :feilutbetalingToMånederTilbake, :feilutbetalingForrigeMåned) " +
                "ON CONFLICT DO NOTHING"
        val params = MapSqlParameterSource(
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
            ),
        )
        namedParameterJdbcTemplate.update(sql, params)
    }

    fun hentInntektsendringerSomSkalHaOppgave(): List<InntektOgVedtakEndring> {
        val sql = "SELECT * FROM inntektsendringer WHERE " +
            "(inntekt_endret_tre_maaneder_tilbake >= 10 AND " +
            "inntekt_endret_to_maaneder_tilbake >= 10 AND " +
            "inntekt_endret_forrige_maaned >= 10) AND " +
            "(feilutbetaling_fire_maaneder_tilbake + feilutbetaling_tre_maaneder_tilbake + feilutbetaling_to_maaneder_tilbake + feilutbetaling_forrige_maaned) > 20000"
        return namedParameterJdbcTemplate.query(sql, inntektsendringerMapper)
    }

    fun hentInntektsendringerForUføretrygd(): List<InntektOgVedtakEndring> {
        val sql = "select * from inntektsendringer where ny_ytelse_type like '%ufoeretrygd%'"
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
)
