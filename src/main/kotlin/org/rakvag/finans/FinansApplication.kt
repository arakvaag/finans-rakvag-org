package org.rakvag.finans

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import java.time.Clock


@SpringBootApplication
@Configuration
@EnableScheduling
class FinansApplication {

	@Bean
	fun userDetailsServiceBean(
			@Value("\${USERNAME}") user: String,
			@Value("\${PASSWORD}") password: String
	): UserDetailsService {

		val users: MutableList<UserDetails> = ArrayList()
		@Suppress("DEPRECATION")
		users.add(
				User.withDefaultPasswordEncoder()
						.username(user)
						.password(password)
						.roles("USER")
						.build()
		)
		return InMemoryUserDetailsManager(users)
	}

	@Bean
	fun klokkeProvider(): KlokkeProvider {
		return KlokkeProvider()
	}
}

open class KlokkeProvider(
	private val clock: Clock = Clock.systemDefaultZone()
) {
	open fun klokke(): Clock {
		return clock
	}
}

fun main(args: Array<String>) {
	runApplication<FinansApplication>(*args)
}
