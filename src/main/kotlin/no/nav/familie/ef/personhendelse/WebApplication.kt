package no.nav.familie.ef.personhendelse

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication
class WebApplication {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplicationBuilder(WebApplication::class.java)
                .run(*args)
        }
    }
}


