package no.nav.familie.ef.personhendelse.client.pdl

data class PdlPersonRequest(val variables: PdlPersonRequestVariables,
                            val query: String)

data class PdlPersonRequestVariables(val ident: String)
