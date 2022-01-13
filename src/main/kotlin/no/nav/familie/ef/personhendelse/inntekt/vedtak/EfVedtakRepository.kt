package no.nav.familie.ef.personhendelse.inntekt.vedtak

import no.nav.familie.kontrakter.felles.ef.EnsligForsørgerVedtakhendelse
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class EfVedtakRepository(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

    fun lagreEfVedtakshendelse(vedtakshendelse: EnsligForsørgerVedtakhendelse) {
        val sql = "INSERT INTO efvedtakhendelse VALUES(:behandlingId, :personIdent, :stønadType)" +
            " ON CONFLICT DO NOTHING"
        val params = MapSqlParameterSource(
            mapOf(
                "behandlingId" to vedtakshendelse.behandlingId,
                "personIdent" to vedtakshendelse.personIdent,
                "stønadType" to vedtakshendelse.stønadType.toString()
            )
        )
        namedParameterJdbcTemplate.update(sql, params)
    }

    fun hentEfVedtakHendelse(personIdent: String): EnsligForsørgerVedtakhendelse? {
        val sql = "SELECT * FROM efvedtakhendelse WHERE person_ident = :personIdent"
        val mapSqlParameterSource = MapSqlParameterSource("personIdent", personIdent)
        return try {
            namedParameterJdbcTemplate.queryForObject(sql, mapSqlParameterSource) { rs: ResultSet, _: Int ->
                EnsligForsørgerVedtakhendelse(
                    rs.getLong("behandling_id"),
                    rs.getString("person_ident"),
                    StønadType.valueOf(rs.getString("stonadstype"))
                )
            }
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    // Kommer til å bli endret til å ta alle som ikke er behandlet.
    fun hentAllePersonerMedVedtak(): List<EnsligForsørgerVedtakhendelse> {
        val sql = "SELECT * FROM efvedtakhendelse"
        val mapSqlParameterSource = MapSqlParameterSource("stonadstype", StønadType.OVERGANGSSTØNAD.toString())
        return try {
            namedParameterJdbcTemplate.query(sql, mapSqlParameterSource) { rs: ResultSet, _: Int ->
                EnsligForsørgerVedtakhendelse(
                    rs.getLong("behandling_id"),
                    rs.getString("person_ident"),
                    StønadType.valueOf(rs.getString("stonadstype"))
                )
            }
        } catch (e: EmptyResultDataAccessException) {
            listOf()
        }
    }
}
