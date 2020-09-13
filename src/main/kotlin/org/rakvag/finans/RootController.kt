package org.rakvag.finans

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController("/")
class RootController(
        private val sbankenClient: SbankenClient
) {

    @GetMapping("")
    fun helloWorld(principal: Principal): ResponseEntity<String> {
        val response = sbankenClient.getAccountInfo()
        return ResponseEntity.ok("Hello world.<br>Response: $response")
    }

}