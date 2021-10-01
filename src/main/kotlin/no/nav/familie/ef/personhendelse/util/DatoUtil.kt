package no.nav.familie.ef.personhendelse.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATO_FORMAT_NORSK = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun LocalDate?.tilNorskDatoformat(): String {
    return this?.format(DATO_FORMAT_NORSK) ?: "ukjent dato"
}