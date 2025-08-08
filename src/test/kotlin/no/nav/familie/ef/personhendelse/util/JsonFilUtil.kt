package no.nav.familie.ef.personhendelse.util

import java.nio.charset.StandardCharsets

class JsonFilUtil {
    companion object {
        fun readResource(name: String): String =
            this::class.java.classLoader
                .getResource(name)!!
                .readText(StandardCharsets.UTF_8)
    }
}
