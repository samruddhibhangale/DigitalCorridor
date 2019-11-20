package com.template

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Our Spring Boot application.
 */
@SpringBootApplication
open class Starter

/**
 * Starts our Spring Boot application.
 */
fun main(args: Array<String>) {
    runApplication<Starter>(*args)
}