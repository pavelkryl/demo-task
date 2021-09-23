package eu.kryl.ataccama.task.model

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id


@Schema(description = "Named connection details")
@Entity
data class ConnectionDetails(

    // do not expose ID over REST, we will use id internally, but for now we pretend that we are using unique 'name' for identification
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = IDENTITY)
    val id: Long,

    @Schema(description = "Unique connection name")
    @Column(unique = true)
    val name: String,

    val hostname: String,

    val port: Int,

    val databaseName: String,

    val username: String,

    val password: String

)
