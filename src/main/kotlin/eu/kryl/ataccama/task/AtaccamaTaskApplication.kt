package eu.kryl.ataccama.task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan("eu.kryl.ataccama.task.configuration")
class AtaccamaTaskApplication

fun main(args: Array<String>) {
    runApplication<AtaccamaTaskApplication>(*args)
}
