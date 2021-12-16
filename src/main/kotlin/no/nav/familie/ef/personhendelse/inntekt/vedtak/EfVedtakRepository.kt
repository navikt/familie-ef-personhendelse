package no.nav.familie.ef.personhendelse.inntekt.vedtak

import no.nav.familie.kontrakter.felles.ef.EnsligForsørgerVedtakhendelse
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime

@Repository
class EfVedtakRepository(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

    fun lagreEfVedtakHendelse(vedtakHendelse: EnsligForsørgerVedtakhendelse) {
        val sql = "INSERT INTO efvedtakhendelse VALUES(:behandlingId, :personIdent, :stønadType, :timestamp)"
        val params = MapSqlParameterSource(
            mapOf(
                "behandlingId" to vedtakHendelse.behandlingId,
                "personIdent" to vedtakHendelse.personIdent,
                "stønadType" to vedtakHendelse.stønadType,
                "timestamp" to LocalDateTime.now()
            )
        )
        namedParameterJdbcTemplate.update(sql, params)
    }

    fun hentEfVedtakHendelse(personIdent: String): EnsligForsørgerVedtakhendelse? {
        val sql = "SELECT * FROM efvedtakhendelse WHERE personIdent = :personIdent"
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
}
