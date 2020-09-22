package org.rakvag.finans

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Suppress("FunctionName")
@Service
class SkedulertSjekkAvDisponibeltBeløp(
        @Value("\${KONTONUMMER_BRUKSKONTO}") private val kontonummer: String,
        @Value("\${SMS_VARSLING_AKTIVERT}") private val smsVarslingAktivert: Boolean,
        private val sbankenClient: SbankenClient,
        private val smsSender: SmsSender,
        private val klokkeProvider: KlokkeProvider
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private var tidspunktSisteSmsVarsel: LocalDateTime? = null

    @Scheduled(initialDelayString = "\${KONTROLL_AV_DISPONIBELT_BELOP_INITIAL_DELAY}",
            fixedDelayString = "\${KONTROLL_AV_DISPONIBELT_BELOP_INTERVALL}")
    fun skedulertSjekkAvSaldo() {
        logger.debug("Kjører skedulert sjekk av saldo")

        val kontoinformasjon = sbankenClient.getAccountInfo()
        val brukskonto = kontoinformasjon.items.find { it.accountNumber == kontonummer }
                ?: throw RuntimeException("Fant ikke kontoen")

        val terskelForVarsling = 1500.00
        val erDisponibeltUnderTerskelForVarsling = brukskonto.available < terskelForVarsling
        logger.debug("Disponibelt beløp under terskel for varsling: $erDisponibeltUnderTerskelForVarsling")
        if (erDisponibeltUnderTerskelForVarsling) {
            val erVarslingTillattNå = erVarslingTillattNå()
            logger.debug("Varsling tillatt nå: $erVarslingTillattNå")
            if (erVarslingTillattNå) {
                logger.info("Sender SMS")
                smsSender.sendSms("Saldo på brukskonto er ${brukskonto.available}, under grense på $terskelForVarsling kr")
                tidspunktSisteSmsVarsel = LocalDateTime.now(klokkeProvider.klokke())
            }
        }
    }

    @Suppress("DuplicatedCode")
    private fun erVarslingTillattNå(): Boolean {
        if (!smsVarslingAktivert) {
            return false
        }

        //Er vi inne i stille-perioden?
        if (setOf(22, 23, 0, 1, 2, 3, 4, 5, 6, 7).contains(LocalDateTime.now(klokkeProvider.klokke()).hour)) {
            return false
        }

        if (tidspunktSisteSmsVarsel == null) {
            return true
        }

        val tidSisteVarselPlussVentetid = tidspunktSisteSmsVarsel!!.plus(Duration.of(3, ChronoUnit.HOURS))
        val tidNå = LocalDateTime.now(klokkeProvider.klokke())
        if (tidSisteVarselPlussVentetid < tidNå) {
            return true
        }

        return false
    }

}