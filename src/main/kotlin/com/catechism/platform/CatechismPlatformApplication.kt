package com.catechism.platform

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CatechismPlatformApplication

fun main(args: Array<String>) {
    runApplication<CatechismPlatformApplication>(*args)
}
