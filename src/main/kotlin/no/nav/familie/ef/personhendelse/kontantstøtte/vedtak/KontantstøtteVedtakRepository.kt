package no.nav.familie.ef.personhendelse.kontantstøtte.vedtak

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class KontantstøtteVedtakRepository(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {
    fun lagreKontantstøttevedtak(behandlingId: String) {
        val sql = "INSERT INTO kontantstotte_vedtakhendelse VALUES(:behandlingId)"
        val params = MapSqlParameterSource(mapOf("behandlingId" to behandlingId))
        namedParameterJdbcTemplate.update(sql, params)
    }

    fun harAlleredeProsessertKontantstøttevedtak(behandlingId: String): Boolean {
        val sql = "SELECT count(*) FROM kontantstotte_vedtakhendelse WHERE behandling_id = :behandlingId"
        val mapSqlParameterSource = MapSqlParameterSource("behandlingId", behandlingId)
        val antallMedBehandlingId: Int =
            namedParameterJdbcTemplate.queryForObject(sql, mapSqlParameterSource, Int::class.java)
                ?: error("Kunne ikke utføre spørring for å sjekke om kontantstøttevedtak allerede er håndtert")
        return antallMedBehandlingId > 0
    }
}
