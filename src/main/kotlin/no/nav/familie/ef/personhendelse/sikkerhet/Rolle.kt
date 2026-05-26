package no.nav.familie.ef.personhendelse.sikkerhet

enum class Rolle {
    FORVALTER,
    APPLICATION,
    ;

    fun authority(): String = "ROLE_$name"
}
