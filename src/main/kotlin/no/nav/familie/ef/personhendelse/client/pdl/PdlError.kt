package no.nav.familie.ef.personhendelse.client.pdl

data class PdlError(
    val message: String,
    val extensions: PdlErrorExtensions?,
)

data class PdlErrorExtensions(val code: String?) {

    fun notFound() = code == "not_found"
}
