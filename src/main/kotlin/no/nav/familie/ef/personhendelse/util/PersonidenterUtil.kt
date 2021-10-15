package no.nav.familie.ef.personhendelse.util

import no.nav.person.pdl.leesah.Personhendelse

fun Personhendelse.identerUtenAktørId() = this.personidenter.filter { it.length == 11 }.toSet()