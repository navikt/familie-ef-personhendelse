package no.nav.familie.ef.personhendelse.inntekt.vedtak

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

    fun lagreInntektsendring(personIdent: String, harNyeVedtak: Boolean, harEndretInntekt: Boolean, inntektEndretProsent: Int) {
        val sql =
            "INSERT INTO inntektsendringer VALUES(:id, :personIdent, :harNyeVedtak, :harEndretInntekt, :prosessertTid, :inntektEndretProsent)" +
                " ON CONFLICT DO NOTHING"
        val params = MapSqlParameterSource(
            mapOf(
                "id" to UUID.randomUUID(),
                "personIdent" to personIdent,
                "harNyeVedtak" to harNyeVedtak,
                "harEndretInntekt" to harEndretInntekt,
                "prosessertTid" to LocalDateTime.now(),
                "inntektEndretProsent" to inntektEndretProsent,
            ),
        )
        namedParameterJdbcTemplate.update(sql, params)
    }

    fun hentInntektsendringer(): List<Inntektsendring> {
        val sql = "SELECT * FROM inntektsendringer WHERE harNyttVedtak = true OR harEndretInntekt = true"
        return namedParameterJdbcTemplate.query(sql, inntektsendringerMapper)
    }

    private val inntektsendringerMapper = { rs: ResultSet, _: Int ->
        Inntektsendring(
            rs.getString("person_ident"),
            rs.getBoolean("harNyttVedtak"),
            rs.getBoolean("harEndretInntekt"),
            rs.getObject("prosessert_tid", LocalDateTime::class.java),
            rs.getInt("inntekt_endret_prosent"),
        )
    }

    fun clearInntektsendringer() {
        val sql = "DELETE FROM inntektsendringer"
        namedParameterJdbcTemplate.update(sql, MapSqlParameterSource())
    }
}

data class Inntektsendring(
    val personIdent: String,
    val harNyttVedtak: Boolean,
    val harEndretInntekt: Boolean,
    val prosessertTid: LocalDateTime,
    val inntektEndretProsent: Int,
)
