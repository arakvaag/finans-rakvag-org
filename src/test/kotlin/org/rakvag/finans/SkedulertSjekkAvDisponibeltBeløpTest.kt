package org.rakvag.finans

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.time.Clock
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId


class SkedulertSjekkAvDisponibeltBeløpTest {

    @Mock
    private lateinit var sbankenClient: SbankenClient
    @Mock
    private lateinit var smsSender: SmsSender
    @Mock
    private lateinit var klokkeProvider: KlokkeProvider

    private lateinit var service: SkedulertSjekkAvDisponibeltBeløp

    private val kontonummer = "12341212345"

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        service = SkedulertSjekkAvDisponibeltBeløp(kontonummer, true, sbankenClient, smsSender, klokkeProvider)
        `when`(klokkeProvider.klokke()).thenReturn(hentKlokkeLåstTilOppgittTime(15))
    }

    @Test
    fun `sender varsel når disponibelt under terskel for varsling, ingen varsling gjort allerede og time på dagen er ok`() {
        //ARRANGE
        val kontoer = listOf(AccountInfo("aa", kontonummer, "dsl",
                "name", "ttt", 250.00, 260.00, 0.00))
        `when`(sbankenClient.getAccountInfo()).thenReturn(GetAccountInfoResponse(1, kontoer))

        //ACT
        service.skedulertSjekkAvSaldo()

        //ASSERT
        verify(sbankenClient).getAccountInfo()
        verify(smsSender).sendSms(anyString())
    }

    @Test
    fun `sender ikke varsel når varsling er gjort nylig`() {
        //ARRANGE
        val kontoer = listOf(AccountInfo("aa", kontonummer, "dsl",
                "name", "ttt", 250.00, 260.00, 0.00))
        `when`(sbankenClient.getAccountInfo()).thenReturn(GetAccountInfoResponse(1, kontoer))

        //Trigger første kjøring - som vil registrere at en varsling nylig er gjort
        service.skedulertSjekkAvSaldo()
        verify(smsSender).sendSms(anyString()) //verifiserer at varslingen skjedde
        reset(smsSender)

        //ACT
        service.skedulertSjekkAvSaldo()

        //ASSERT
        verifyNoInteractions(smsSender)
    }

    @Test
    fun `sender varsel når forrige varsling ble gjort for mer enn 3 timer siden`() {
        //ARRANGE
        val kontoer = listOf(AccountInfo("aa", kontonummer, "dsl",
                "name", "ttt", 250.00, 260.00, 0.00))
        `when`(sbankenClient.getAccountInfo()).thenReturn(GetAccountInfoResponse(1, kontoer))

        //Trigger første kjøring - som vil registrere at en varsling nylig er gjort
        `when`(klokkeProvider.klokke()).thenReturn(hentKlokkeLåstTilOppgittTime(12))
        service.skedulertSjekkAvSaldo()
        verify(smsSender).sendSms(anyString()) //verifiserer at varslingen skjedde
        reset(smsSender)

        //ACT
        `when`(klokkeProvider.klokke()).thenReturn(hentKlokkeLåstTilOppgittTime(16))
        service.skedulertSjekkAvSaldo()

        //ASSERT
        verify(smsSender).sendSms(anyString())
    }

    @Test
    fun `sender ikke varsel når timen er i stille periode`() {
        //ARRANGE
        val kontoer = listOf(AccountInfo("aa", kontonummer, "dsl",
                "name", "ttt", 250.00, 260.00, 0.00))
        `when`(sbankenClient.getAccountInfo()).thenReturn(GetAccountInfoResponse(1, kontoer))
        `when`(klokkeProvider.klokke()).thenReturn(hentKlokkeLåstTilOppgittTime(2))

        //ACT
        service.skedulertSjekkAvSaldo()

        //ASSERT
        verifyNoInteractions(smsSender)
    }

    @Test
    fun `sender ikke varsel når sms-varsling er deaktivert`() {
        //ARRANGE
        service = SkedulertSjekkAvDisponibeltBeløp(kontonummer,
                false,
                sbankenClient, smsSender, klokkeProvider)

        val kontoer = listOf(AccountInfo("aa", kontonummer, "dsl",
                "name", "ttt", 250.00, 260.00, 0.00))
        `when`(sbankenClient.getAccountInfo()).thenReturn(GetAccountInfoResponse(1, kontoer))
        `when`(klokkeProvider.klokke()).thenReturn(hentKlokkeLåstTilOppgittTime(2))

        //ACT
        service.skedulertSjekkAvSaldo()

        //ASSERT
        verifyNoInteractions(smsSender)
    }

    private fun hentKlokkeLåstTilOppgittTime(time: Int): Clock {
        val dateTime = LocalDateTime.of(2020, Month.SEPTEMBER, 15, time, 0, 0)
        val instant = dateTime.atZone(ZoneId.of("Europe/Oslo")).toInstant()
        val klokke = Clock.fixed(instant, ZoneId.systemDefault())
        return klokke
    }
}