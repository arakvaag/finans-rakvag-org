package org.rakvag.finans

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController("/")
class RootController(
        @Value("\${KONTONUMMER}") private val kontonummer: String,
        private val sbankenClient: SbankenClient
) {

    @GetMapping("")
    fun helloWorld(principal: Principal): ResponseEntity<String> {
        val kontoinformasjon = sbankenClient.getAccountInfo()
        val brukskonto = kontoinformasjon.items.find { it.accountNumber == kontonummer }
                ?: throw RuntimeException("Fant ikke kontoen")
        val formatertKonto = formaterKonto(brukskonto)
        return ResponseEntity.ok("Konto $formatertKonto har disponibelt ${brukskonto.available} kr.")
    }

    private fun formaterKonto(konto: AccountInfo): String {
        val kontonummer = "${kontonummer.subSequence(0, 4)} ${kontonummer.subSequence(4, 6)} ${kontonummer.subSequence(6, 11)}"

        return "$kontonummer (${konto.name})"
    }

}