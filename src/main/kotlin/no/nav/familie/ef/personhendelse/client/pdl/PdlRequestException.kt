package no.nav.familie.ef.personhendelse.client.pdl

open class PdlRequestException(melding: String? = null) : Exception(melding)

class PdlNotFoundException : PdlRequestException()
