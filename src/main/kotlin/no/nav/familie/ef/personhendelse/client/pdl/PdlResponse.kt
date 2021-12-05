package no.nav.familie.ef.personhendelse.client.pdl

data class PdlResponse<T>(
    val data: T,
    val errors: List<PdlError>?
) {

    fun harFeil(): Boolean {
        return errors != null && errors.isNotEmpty()
    }

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }
}
