package no.nav.familie.ef.personhendelse.sikkerhet

enum class Rolle {
    FORVALTER,
    PROSESSERING,
    APPLICATION,
    ;

    fun authority(): String = "ROLE_$name"
}
