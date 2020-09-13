package org.rakvag.finans

import com.fasterxml.jackson.databind.ObjectMapper
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
    private val accountServiceUrl = "https://api.sbanken.no/exec.bank/api/v1/accounts/"

    fun getAccountInfo(): GetAccountInfoResponse {
        val basicAuth = getBase64AuthString(sbankenClientId, sbankenPassword)
        val httpClient = HttpClient.newHttpClient()
        val token = getAccessToken(httpClient, basicAuth)
        val serviceRequest = HttpRequest.newBuilder(URI(accountServiceUrl))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $token")
                .header("customerId", sbankenCustomerId)
                .build()
        val jsonResponse = httpClient.send(serviceRequest, HttpResponse.BodyHandlers.ofString()).body()
        return parseGetAccountInfoResponseJson(jsonResponse, objectMapper)
    }

    private fun getBase64AuthString(clientId: String, secret: String): String {
        val clientIdEncoded = URLEncoder.encode(clientId, StandardCharsets.UTF_8)
        val secretEncoded = URLEncoder.encode(secret, StandardCharsets.UTF_8)
        val base64AuthStringBytes = Base64.getEncoder().encode("$clientIdEncoded:$secretEncoded".toByteArray(StandardCharsets.UTF_8))
        return String(base64AuthStringBytes, StandardCharsets.UTF_8)
    }

    private fun getAccessToken(httpClient: HttpClient, basicAuth: String): String {
        val serviceRequest = HttpRequest.newBuilder(URI(identityServerUrl))
                .header("Accept", "application/json")
                .header("Authorization", "Basic $basicAuth")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofByteArray("grant_type=client_credentials".toByteArray()))
                .build()
        val response = httpClient.send(serviceRequest, HttpResponse.BodyHandlers.ofString())
        return (objectMapper.readValue(response.body(), Map::class.java) as Map<*, *>)["access_token"] as String?
                ?: throw RuntimeException("Feil ved lesing av json")
    }

    private fun parseGetAccountInfoResponseJson(jsonResponse: String, objectMapper: ObjectMapper): GetAccountInfoResponse {
        return objectMapper.readValue(jsonResponse, GetAccountInfoResponse::class.java)
    }

}

data class GetAccountInfoResponse(
        val availableItems: Int,
        val items: List<AccountInfo>,
        val errorType: String?,
        val isError: Boolean,
        val errorCode: String?,
        val errorMessage: String?,
        val traceId: String?
)

data class AccountInfo(
        val accountId: String,
        val accountNumber: String,
        val ownerCustomerId: String,
        val name: String,
        val accountType: String,
        val available: Double,
        val balance: Double,
        val creditLimit: Double
)

