package no.nav.familie.ef.personhendelse.common

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity


@Component
class AzureClient(
    @Value("\${AZURE_OPENID_CONFIG_TOKEN_ENDPOINT}")
    private val url: String,
    @Value("\${AZURE_APP_CLIENT_ID}")
    private val clientId: String,
    @Value("\${AZURE_APP_CLIENT_SECRET}")
    private val clientSecret: String,
) {

    fun hentToken(scope: String): String {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val map: MultiValueMap<String, String> = LinkedMultiValueMap()
        map.add("client_id", clientId)
        map.add("scope", scope)
        map.add("client_secret", clientSecret)
        map.add("grant_type", "client_credentials")


        val request = HttpEntity<MultiValueMap<String, String>>(map, headers)

        val restTemplate = RestTemplate()
        val response = restTemplate.postForEntity<Token>(url, request)
        if (response.body == null) {
            throw Exception("Fikk ikke hentet ut token")
        }
        return response.body!!.token
    }
}