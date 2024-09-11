package no.nav.familie.ef.personhendelse.utsattoppgave

import no.nav.familie.ef.personhendelse.handler.PersonhendelseType
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
class UtsattOppgaveRepository(
    val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun lagreOppgave(
        hendelseId: UUID,
        personhendelseType: PersonhendelseType,
        endringstype: String,
        personIdent: String,
        beskrivelse: String,
        hendelsesTid: LocalDateTime,
    ) {
        val sql =
            "INSERT INTO utsattoppgave VALUES(:hendelsesId, :personIdent, :beskrivelse, :personhendelseType, :endringstype, :hendelsestid, :opprettetoppgavetid) ON CONFLICT DO NOTHING"
        val params =
            MapSqlParameterSource(
                mapOf(
                    "hendelsesId" to hendelseId,
                    "personIdent" to personIdent,
                    "beskrivelse" to beskrivelse,
                    "personhendelseType" to personhendelseType.name,
                    "endringstype" to endringstype,
                    "hendelsestid" to hendelsesTid,
                    "opprettetoppgavetid" to null,
                ),
            )
        namedParameterJdbcTemplate.update(sql, params)
    }

    fun settOppgaveTilUtført(hendelseId: UUID) {
        val sql =
            "UPDATE utsattoppgave SET opprettetoppgavetid = :opprettetoppgavetid WHERE hendelse_id=:hendelseId"
        val params =
            MapSqlParameterSource(
                mapOf(
                    "hendelseId" to hendelseId,
                    "opprettetoppgavetid" to LocalDateTime.now(),
                ),
            )
        namedParameterJdbcTemplate.update(sql, params)
    }

    fun hentIkkeOpprettedeForsinkedeOppgaverOverEnUkeTilbakeITid(): List<UtsattOppgave> {
        val sql =
            "SELECT * FROM utsattoppgave WHERE hendelsestid <= (NOW() - INTERVAL '1 week') AND opprettetoppgavetid IS NULL"
        return namedParameterJdbcTemplate.query(sql, utsattOppgaveMapper)
    }

    private val utsattOppgaveMapper = { rs: ResultSet, _: Int ->
        UtsattOppgave(
            UUID.fromString(rs.getString("hendelse_id")),
            rs.getString("person_id"),
            rs.getString("beskrivelse"),
            PersonhendelseType.valueOf(rs.getString("personhendelsetype")),
            Endringstype.valueOf(rs.getString("endringstype")),
            rs.getObject("hendelsestid", LocalDateTime::class.java),
            rs.getObject("opprettetoppgavetid", LocalDateTime::class.java),
        )
    }
}
