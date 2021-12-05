package no.nav.familie.ef.personhendelse

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication
class Application {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplicationBuilder(Application::class.java)
                .run(*args)
        }
    }
}
