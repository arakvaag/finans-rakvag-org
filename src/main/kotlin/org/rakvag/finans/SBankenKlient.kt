package org.rakvag.finans

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.*

//Basert på https://github.com/Sbanken/api-examples/blob/master/JavaSampleApplication/src/main/java/no/ohuen/sbanken/SbankenClient.java

@Service
class SbankenClient(
        @Value("\${SBANKEN_CLIENT_ID}") private val sbankenClientId: String,
        @Value("\${SBANKEN_PASSWORD}") private val sbankenPassword: String,
        @Value("\${SBANKEN_CUSTOMER_ID}") private val sbankenCustomerId: String,
        private val objectMapper: ObjectMapper
) {

    private val identityServerUrl = "https://auth.sbanken.no/identityserver/connect/token"
    private val accountsServiceUrl = "https://api.sbanken.no/exec.bank/api/v1/Accounts"
    private val paymentsServiceUrl = "https://api.sbanken.no/exec.bank/api/v1/Payments"

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun getAccountInfo(): GetAccountInfoResponse {
        val httpClient = HttpClient.newHttpClient()
        val token = getAccessToken(httpClient)
        val serviceRequest = HttpRequest.newBuilder(URI(accountsServiceUrl))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $token")
                .header("customerId", sbankenCustomerId)
                .build()
        val jsonResponse = httpClient.send(serviceRequest, HttpResponse.BodyHandlers.ofString()).body()
        return objectMapper.readValue(jsonResponse, GetAccountInfoResponse::class.java)
    }

    fun getPayments(accountId: String): GetPaymentsResponse {
        val httpClient = HttpClient.newHttpClient()
        val token = getAccessToken(httpClient)
        val serviceRequest = HttpRequest.newBuilder(URI("$paymentsServiceUrl/$accountId"))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $token")
                .header("customerId", sbankenCustomerId)
                .build()
        val jsonResponse = httpClient.send(serviceRequest, HttpResponse.BodyHandlers.ofString()).body()
        return objectMapper.readValue(jsonResponse, GetPaymentsResponse::class.java)
    }

    fun getAccessToken(httpClient: HttpClient = HttpClient.newHttpClient()): String {
        val clientIdEncoded = URLEncoder.encode(sbankenClientId, StandardCharsets.UTF_8)
        val passwordEncoded = URLEncoder.encode(sbankenPassword, StandardCharsets.UTF_8)
        val base64AuthStringBytes = Base64.getEncoder().encode("$clientIdEncoded:$passwordEncoded".toByteArray(StandardCharsets.UTF_8))
        val basicAuth = String(base64AuthStringBytes, StandardCharsets.UTF_8)

        val serviceRequest = HttpRequest.newBuilder(URI(identityServerUrl))
                .header("Accept", "application/json")
                .header("Authorization", "Basic $basicAuth")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofByteArray("grant_type=client_credentials".toByteArray()))
                .build()
        val response = httpClient.send(serviceRequest, HttpResponse.BodyHandlers.ofString())

        val responseMap = objectMapper.readValue(response.body(), Map::class.java) as Map<*, *>
        if (!responseMap.containsKey("access_token")) {
            logger.error("Respons ved henting av access-token manglet en key access_token. Key-ene i Json var:\n${responseMap.keys}")
            throw RuntimeException("Feil ved lesing av json")
        }
        return responseMap["access_token"] as String
    }

}

