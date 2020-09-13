package org.rakvag.finans

import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Message
import com.twilio.type.PhoneNumber
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class SmsSender(
        @Value("\${TWILIO_ACCOUNT_SID}") private val twilioAccountSid: String,
        @Value("\${TWILIO_AUTH_TOKEN}") private val twilioAuthToken: String,
        @Value("\${TWILIO_FROM_NUMBER}") private val twilioFromNumber: String,
        @Value("\${TWILIO_TO_NUMBER}") private val twilioToNumber: String
) {

    init {
        Twilio.init(twilioAccountSid, twilioAuthToken)
    }

    fun sendSms(melding: String) {
        Message.creator(
                PhoneNumber(twilioToNumber),
                PhoneNumber(twilioFromNumber),
                melding
        ).create()
    }
}