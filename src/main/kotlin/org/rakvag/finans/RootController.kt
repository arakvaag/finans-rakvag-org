package org.rakvag.finans

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController("/")
class RootController {

    @GetMapping("")
    fun helloWorld(): ResponseEntity<String> {
        return ResponseEntity.ok("Hello world")
    }

}