package no.nav.familie.ef.personhendelse.handler

enum class PersonhendelseType(val hendelsetype: String) {
    DØDFØDT_BARN("DOEDFOEDT_BARN_V1"),
    DØDSFALL("DOEDSFALL_V1"),
    SIVILSTAND("SIVILSTAND_V1"),
    UTFLYTTING_FRA_NORGE("UTFLYTTING_FRA_NORGE");

}