package org.rakvag.finans

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
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

    private lateinit var service: SkedulertSjekkAvDisponibeltBeløp

    private val kontonummer = "12341212345"

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun test() {
        //ARRANGE
        service = SkedulertSjekkAvDisponibeltBeløp(
                kontonummer,
                true,
                sbankenClient,
                smsSender,
                hentKlokkeLåstTilOppgittTime(15)
        )
        val kontoer = listOf(AccountInfo("aa", kontonummer, "dsl",
                "name", "ttt", 250.00, 260.00, 0.00))
        `when`(sbankenClient.getAccountInfo()).thenReturn(GetAccountInfoResponse(1, kontoer))

        //ACT
        service.skedulertSjekkAvSaldo()

        //ASSERT
        verify(smsSender).sendSms(anyString())
    }

    private fun hentKlokkeLåstTilOppgittTime(time: Int): Clock {
        val dateTime = LocalDateTime.of(2020, Month.SEPTEMBER, 15, time, 0, 0)
        val instant = dateTime.atZone(ZoneId.of("Europe/Oslo")).toInstant()
        val klokke = Clock.fixed(instant, ZoneId.systemDefault())
        return klokke
    }
}