package no.nav.familie.ef.personhendelse.personhendelsemapping

import no.nav.familie.ef.personhendelse.Hendelse
import no.nav.person.pdl.leesah.Endringstype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID

@Repository
class PersonhendelseRepository(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun lagrePersonhendelse(hendelsesId: UUID, oppgaveId: Long, endringstype: Endringstype) {
        val sql = "INSERT INTO hendelse VALUES(:hendelsesId, :oppgaveId, :endringsType, :timestamp)"
        val params = MapSqlParameterSource(
            mapOf(
                "hendelsesId" to hendelsesId,
                "oppgaveId" to oppgaveId,
                "endringsType" to endringstype.name,
                "timestamp" to LocalDateTime.now()
            )
        )
        namedParameterJdbcTemplate.update(sql, params)
    }

    fun hentHendelse(hendelsesId: UUID): Hendelse? {
        val sql = "SELECT * FROM hendelse WHERE hendelse_id = :hendelsesId"
        val mapSqlParameterSource = MapSqlParameterSource("hendelsesId", hendelsesId)
        return namedParameterJdbcTemplate.queryForObject(sql, mapSqlParameterSource) { rs: ResultSet, _: Int ->
            Hendelse(
                UUID.fromString(rs.getString("hendelse_id")),
                rs.getLong("oppgave_id"),
                Endringstype.valueOf(rs.getString("endringstype")),
                rs.getTimestamp("opprettet_tid").toLocalDateTime()
            )
        }
    }

}