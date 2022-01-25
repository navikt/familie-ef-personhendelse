package no.nav.familie.ef.personhendelse.inntekt.vedtak

import no.nav.familie.kontrakter.felles.ef.EnsligForsørgerVedtakhendelse
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.YearMonth

@Repository
class EfVedtakRepository(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

    fun lagreEfVedtakshendelse(vedtakshendelse: EnsligForsørgerVedtakhendelse) {
        val sql = "INSERT INTO efvedtakhendelse VALUES(:behandlingId, :personIdent, :stønadType, :aar_maaned_prosessert, :versjon)" +
            " ON CONFLICT DO NOTHING"
        val params = MapSqlParameterSource(
            mapOf(
                "behandlingId" to vedtakshendelse.behandlingId,
                "personIdent" to vedtakshendelse.personIdent,
                "stønadType" to vedtakshendelse.stønadType.toString(),
                "aar_maaned_prosessert" to YearMonth.now().toString(),
                "versjon" to 1
            )
        )
        namedParameterJdbcTemplate.update(sql, params)
    }

    fun hentEfVedtakHendelse(personIdent: String): VedtakhendelseInntektberegning? {
        val sql = "SELECT * FROM efvedtakhendelse WHERE person_ident = :personIdent"
        val mapSqlParameterSource = MapSqlParameterSource("personIdent", personIdent)
        return try {
            namedParameterJdbcTemplate.queryForObject(sql, mapSqlParameterSource) { rs: ResultSet, _: Int ->
                VedtakhendelseInntektberegning(
                    rs.getLong("behandling_id"),
                    rs.getString("person_ident"),
                    StønadType.valueOf(rs.getString("stonadstype")),
                    YearMonth.parse(rs.getString("aar_maaned_prosessert")),
                    rs.getInt("versjon")
                )
            }
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    fun hentAllePersonerMedVedtak(): List<VedtakhendelseInntektberegning> {
        val sql = "SELECT * FROM efvedtakhendelse"
        val mapSqlParameterSource = MapSqlParameterSource("stonadstype", StønadType.OVERGANGSSTØNAD.toString())
        return try {
            namedParameterJdbcTemplate.query(sql, mapSqlParameterSource) { rs: ResultSet, _: Int ->
                VedtakhendelseInntektberegning(
                    rs.getLong("behandling_id"),
                    rs.getString("person_ident"),
                    StønadType.valueOf(rs.getString("stonadstype")),
                    YearMonth.parse(rs.getString("aar_maaned_prosessert")),
                    rs.getInt("versjon")
                )
            }
        } catch (e: EmptyResultDataAccessException) {
            listOf()
        }
    }

    fun hentPersonerMedVedtakIkkeBehandlet(): List<VedtakhendelseInntektberegning> {
        val sql = "SELECT * FROM efvedtakhendelse WHERE aar_maaned_prosessert != '${YearMonth.now()}'"
        val mapSqlParameterSource = MapSqlParameterSource("stonadstype", StønadType.OVERGANGSSTØNAD.toString())
        return try {
            namedParameterJdbcTemplate.query(sql, mapSqlParameterSource) { rs: ResultSet, _: Int ->
                VedtakhendelseInntektberegning(
                    rs.getLong("behandling_id"),
                    rs.getString("person_ident"),
                    StønadType.valueOf(rs.getString("stonadstype")),
                    YearMonth.parse(rs.getString("aar_maaned_prosessert")),
                    rs.getInt("versjon")
                )
            }
        } catch (e: EmptyResultDataAccessException) {
            listOf()
        }
    }

    fun oppdaterAarMaanedProsessert(personIdent: String) {
        oppdaterAarMaanedProsessert(personIdent, YearMonth.now())
    }

    fun oppdaterAarMaanedProsessert(personIdent: String, yearMonth: YearMonth) {
        val sql = "UPDATE efvedtakhendelse SET aar_maaned_prosessert = :yearMonth " +
            "WHERE person_ident = :personIdent"
        val mapSqlParameterSource = MapSqlParameterSource("personIdent", personIdent)
            .addValue("yearMonth", yearMonth.toString())

        namedParameterJdbcTemplate.update(sql, mapSqlParameterSource).takeIf { it == 1 }
            ?: error("Kunne ikke oppdatere efvedtakhendelse")
    }
}
