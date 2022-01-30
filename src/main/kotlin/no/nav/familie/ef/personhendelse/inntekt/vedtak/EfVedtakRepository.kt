package no.nav.familie.ef.personhendelse.inntekt.vedtak

import no.nav.familie.kontrakter.felles.ef.EnsligForsørgerVedtakhendelse
import no.nav.familie.kontrakter.felles.ef.StønadType
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

    fun hentAllePersonerMedVedtak(): List<VedtakhendelseInntektberegning> {
        val sql = "SELECT MAX(behandling_id) as behandling_id, person_ident, stonadstype, aar_maaned_prosessert, versjon FROM efvedtakhendelse GROUP BY person_ident, stonadstype, aar_maaned_prosessert, versjon ORDER BY behandling_id"
        val mapSqlParameterSource = MapSqlParameterSource("stonadstype", StønadType.OVERGANGSSTØNAD.toString())
        return namedParameterJdbcTemplate.query(sql, mapSqlParameterSource, vedtakhendelseInntektberegningMapper)
    }

    fun hentPersonerMedVedtakIkkeBehandlet(): List<VedtakhendelseInntektberegning> {
        val sql = "SELECT * FROM efvedtakhendelse WHERE aar_maaned_prosessert != '${YearMonth.now()}'"
        val mapSqlParameterSource = MapSqlParameterSource("stonadstype", StønadType.OVERGANGSSTØNAD.toString())
        return namedParameterJdbcTemplate.query(sql, mapSqlParameterSource, vedtakhendelseInntektberegningMapper)
    }

    private val vedtakhendelseInntektberegningMapper = { rs: ResultSet, _: Int ->
        VedtakhendelseInntektberegning(
            rs.getLong("behandling_id"),
            rs.getString("person_ident"),
            StønadType.valueOf(rs.getString("stonadstype")),
            YearMonth.parse(rs.getString("aar_maaned_prosessert")),
            rs.getInt("versjon")
        )
    }

    fun oppdaterAarMaanedProsessert(personIdent: String, yearMonth: YearMonth = YearMonth.now()) {
        val sql = "UPDATE efvedtakhendelse SET aar_maaned_prosessert = :yearMonth " +
            "WHERE person_ident = :personIdent"
        val mapSqlParameterSource = MapSqlParameterSource("personIdent", personIdent)
            .addValue("yearMonth", yearMonth.toString())

        namedParameterJdbcTemplate.update(sql, mapSqlParameterSource)
    }
}
