package org.rakvag.finans

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Suppress("NonAsciiCharacters", "LocalVariableName")
@RestController("/")
class RootController(
        @Value("\${KONTONUMMER}") private val kontonummer: String,
        private val sbankenClient: SbankenClient,
        private val smsSender: SmsSender
) {

    private var tidspunktSisteSmsVarsel: LocalDateTime? = null

    private val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("")
    fun getRoot(principal: Principal): ResponseEntity<String> {
        val kontoinformasjon = sbankenClient.getAccountInfo()
        val brukskonto = kontoinformasjon.items.find { it.accountNumber == kontonummer }
                ?: throw RuntimeException("Fant ikke kontoen")

        val melding = "Konto ${formaterKonto(brukskonto)} har disponibelt ${brukskonto.available} kr."
        return ResponseEntity.ok(melding)
    }

    @Scheduled(fixedDelay = 60*1000)
    fun skedulertSjekkAvSaldo() {
        logger.debug("Kjører skedulert sjekk av saldo")
        val kontoinformasjon = sbankenClient.getAccountInfo()
        val brukskonto = kontoinformasjon.items.find { it.accountNumber == kontonummer }
                ?: throw RuntimeException("Fant ikke kontoen")
        val terskelForVarsling = 15000.00
        if (brukskonto.available < terskelForVarsling) {
            if (erVarslingTillattNaa()) {
                logger.info("Sender SMS")
                smsSender.sendSms("Saldo på brukskonto er ${brukskonto.available}, under grense på $terskelForVarsling kr")
            }
        }
    }

    private fun erVarslingTillattNaa(): Boolean {
        if (setOf(22, 23, 0, 1, 2, 3, 4, 5, 6, 7).contains(LocalDateTime.now().hour)) {
            return false
        }

        if (tidspunktSisteSmsVarsel == null) {
            return true
        }

        val minimumVentetidFørNyttSmsVarsel = Duration.of(3, ChronoUnit.HOURS)
        if (tidspunktSisteSmsVarsel!!.plus(minimumVentetidFørNyttSmsVarsel) < LocalDateTime.now()) {
            return true
        }

        return false
    }

    private fun formaterKonto(konto: AccountInfo): String {
        val kontonummer = "${kontonummer.subSequence(0, 4)} ${kontonummer.subSequence(4, 6)} ${kontonummer.subSequence(6, 11)}"

        return "$kontonummer (${konto.name})"
    }

}