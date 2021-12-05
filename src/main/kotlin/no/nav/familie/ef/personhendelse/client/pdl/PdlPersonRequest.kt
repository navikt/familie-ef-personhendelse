package no.nav.familie.ef.personhendelse.client.pdl

data class PdlPersonRequest<T>(
    val variables: T,
    val query: String
)

data class PdlPersonRequestVariables(val ident: String)

data class PdlIdentRequestVariables(
    val ident: String,
    val gruppe: String = "FOLKEREGISTERIDENT",
    val historikk: Boolean = true
)
