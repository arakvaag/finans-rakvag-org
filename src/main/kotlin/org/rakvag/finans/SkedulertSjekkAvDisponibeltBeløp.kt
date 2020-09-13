package org.rakvag.finans

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class SkedulertSjekkAvDisponibeltBeløp(
        @Value("\${KONTONUMMER}") private val kontonummer: String,
        @Value("\${SMS_VARSLING_AKTIVERT}") private val smsVarslingAktivert: Boolean,
        private val sbankenClient: SbankenClient,
        private val smsSender: SmsSender,
        private val clock: Clock
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private var tidspunktSisteSmsVarsel: LocalDateTime? = null

    @Scheduled(initialDelayString = "\${INITAL_DELAY_FOR_KONTROLL_AV_DISPONIBELT_BELOP}",
            fixedDelayString = "\${INTERVALL_MELLOM_KONTROLL_AV_DISPONIBELT_BELOP}")
    fun skedulertSjekkAvSaldo() {
        logger.debug("Kjører skedulert sjekk av saldo")
        val kontoinformasjon = sbankenClient.getAccountInfo()
        val brukskonto = kontoinformasjon.items.find { it.accountNumber == kontonummer }
                ?: throw RuntimeException("Fant ikke kontoen")
        val terskelForVarsling = 1500.00
        if (brukskonto.available < terskelForVarsling) {
            if (erVarslingTillattNaa()) {
                logger.info("Sender SMS")
                smsSender.sendSms("Saldo på brukskonto er ${brukskonto.available}, under grense på $terskelForVarsling kr")
            }
        }
    }

    private fun erVarslingTillattNaa(): Boolean {
        if (!smsVarslingAktivert) {
            return false
        }

        if (setOf(22, 23, 0, 1, 2, 3, 4, 5, 6, 7).contains(LocalDateTime.now(clock).hour)) {
            return false
        }

        if (tidspunktSisteSmsVarsel == null) {
            return true
        }

        val minimumVentetidFørNyttSmsVarsel = Duration.of(3, ChronoUnit.HOURS)
        if (tidspunktSisteSmsVarsel!!.plus(minimumVentetidFørNyttSmsVarsel) < LocalDateTime.now(clock)) {
            return true
        }

        return false
    }

}