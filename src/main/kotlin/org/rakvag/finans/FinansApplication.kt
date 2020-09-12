package org.rakvag.finans

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FinansApplication

fun main(args: Array<String>) {
	runApplication<FinansApplication>(*args)
}
