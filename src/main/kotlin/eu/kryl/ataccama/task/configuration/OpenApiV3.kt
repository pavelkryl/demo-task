package eu.kryl.ataccama.task.configuration

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * Generated swagger docs available at: http://localhost:8080/swagger-ui/
 * Open API v3 docs is at http://localhost:8080/v3/api-docs
 */
@Configuration
class OpenApiV3{


    @Bean
    fun customOpenAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info().title("Ataccama DB introspection API")
                    .version("0.0.1")
                    .description("Ataccama DB introspection Backend API - server model")
            )

}

