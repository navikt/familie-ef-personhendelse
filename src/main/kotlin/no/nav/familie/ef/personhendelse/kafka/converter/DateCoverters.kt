package no.nav.familie.ef.personhendelse.kafka.converter

import com.expediagroup.graphql.client.converter.ScalarConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Suppress("unused") // brukt i pom.xml
class LocalDateConverter : ScalarConverter<LocalDate> {

    override fun toJson(value: LocalDate): Any = value.format(DateTimeFormatter.ISO_LOCAL_DATE)
    override fun toScalar(rawValue: Any): LocalDate = LocalDate.parse(rawValue.toString())
}

@Suppress("unused") // brukt i pom.xml
class LocalDateTimeConverter : ScalarConverter<LocalDateTime> {

    override fun toJson(value: LocalDateTime): Any = value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    override fun toScalar(rawValue: Any): LocalDateTime = LocalDateTime.parse(rawValue.toString())
}
