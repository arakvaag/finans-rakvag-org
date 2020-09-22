package org.rakvag.finans

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Suppress("FunctionName")
@Service
class SkedulertSjekkAvDekningForKommendeBetalinger(
        @Value("\${KONTONUMMER_LOENNSKONTO}") private val kontonummer: String,
        @Value("\${SMS_VARSLING_AKTIVERT}") private val smsVarslingAktivert: Boolean,
        @Value("\${LOENNINGSDAG}") private val lønningsdag: Int,
        private val sbankenClient: SbankenClient,
        private val smsSender: SmsSender,
        private val klokkeProvider: KlokkeProvider
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private var tidspunktSisteSmsVarsel: LocalDateTime? = null

    @Scheduled(initialDelayString = "\${KONTROLL_AV_DEKNING_FOR_KOMMENDE_BETALINGER_INITIAL_DELAY}",
            fixedDelayString = "\${KONTROLL_AV_DEKNING_FOR_KOMMENDE_BETALINGER_INTERVALL}")
    fun skedulertKontrollAvDekningForKommendeBetalinger() {
        logger.debug("Kjører skedulert kontroll av dekning for kommende betalinger")

        val kontoinformasjon = sbankenClient.getAccountInfo()
        val lønnskonto = kontoinformasjon.items.find { it.accountNumber == kontonummer }
                ?: throw RuntimeException("Fant ikke kontoen")

        val payments = sbankenClient.getPayments(lønnskonto.accountId).items
        val sumTilForfallTilNesteLønning = finnSumTilForfallFremTilOppgittDato(payments, finnDatoNesteLønning(lønningsdag))

        val minimumBuffer = BigDecimal("500.00")
        val bufferMellomDisponibeltOgTilForfall = BigDecimal(lønnskonto.available - sumTilForfallTilNesteLønning).setScale(2, RoundingMode.HALF_UP)
        val erBufferUnderTerskelForVarsling = bufferMellomDisponibeltOgTilForfall < minimumBuffer
        logger.debug("Er buffer mellom disponibelt på lønnskonto og sum beløp til forfall for lavt?: $erBufferUnderTerskelForVarsling")
        if (erBufferUnderTerskelForVarsling) {
            val erVarslingTillattNå = erVarslingTillattNå()
            logger.debug("Varsling tillatt nå: $erVarslingTillattNå")
            if (erVarslingTillattNå) {
                logger.info("Sender SMS")
                smsSender.sendSms("Disponibelt på lønnskonto minus sum forfall til neste lønning er $bufferMellomDisponibeltOgTilForfall.")
                tidspunktSisteSmsVarsel = LocalDateTime.now(klokkeProvider.klokke())
            }
        }
    }

    private fun finnSumTilForfallFremTilOppgittDato(payments: List<Payment>, datoNesteLønning: LocalDate): Double {
        val paymentsTilForfallFørLønning = payments.filter { it.dueDate.toLocalDate().isBefore(datoNesteLønning) }.filter { it.isActive }
        return paymentsTilForfallFørLønning.sumByDouble { it.amount }
    }

    private fun finnDatoNesteLønning(lønningsdag: Int): LocalDate {
        return if(LocalDate.now().dayOfMonth <= lønningsdag) {
            LocalDate.now().withDayOfMonth(12)
        } else {
            LocalDate.now().plusMonths(1).withDayOfMonth(12)
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