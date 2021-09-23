package eu.kryl.ataccama.task.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("app")
@ConstructorBinding
class RestConfig(
    val baseUrl: String
)
