package eu.kryl.ataccama.task.rest

import eu.kryl.ataccama.task.configuration.RestConfig
import eu.kryl.ataccama.task.exception.ConnectionAlreadyExistsException
import eu.kryl.ataccama.task.exception.ConnectionNotFoundException
import eu.kryl.ataccama.task.model.ConnectionDetails
import eu.kryl.ataccama.task.repository.ConnectionDetailsRepository
import eu.kryl.ataccama.task.service.DatabaseIntrospectionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import javax.transaction.Transactional

/**
 * Light RESTful layer.
 * We might use spring data rest - but it generates unnecessary garbage.
 * I expect that this layer grows in complexity, then it would be useful to create a separate manager
 * upon which the call execution is delegated.
 */
@RestController
@Tags(Tag(name = "Connection Management"))
class ConnectionDetailsController(
    private val repo: ConnectionDetailsRepository,
    private val manager: DatabaseIntrospectionService,
    private val config: RestConfig
) {

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // CRUD about connection details

    @Operation(summary = "Sets up new connection details.")
    @PostMapping("/connection")
    @ResponseStatus(CREATED)
    fun createConnection(@RequestBody connection: ConnectionDetails, ): ResponseEntity<ConnectionDetails> {
        val existingRecord = repo.findByName(connection.name)
        if (existingRecord != null) {
            throw ConnectionAlreadyExistsException(connection.name)
        } // else:
        repo.save(connection)
        return ResponseEntity.status(CREATED)
            .headers(HttpHeaders().also { it.set(HttpHeaders.LOCATION, config.baseUrl + "/connection/${connection.name}") })
            .body(connection)
    }

    @Operation(summary = "Retrieved named connection details.")
    @GetMapping("/connection/{connectionName}", produces = ["application/json"])
    fun getConnection(
        @PathVariable connectionName: String
    ): ConnectionDetails =
        repo.findByName(connectionName) ?: throw ConnectionNotFoundException(connectionName)

    @Operation(summary = "Update connection details.")
    @PutMapping("/connection/{connectionName}")
    fun updateConnection(
        @PathVariable connectionName: String,
        @RequestBody connection: ConnectionDetails
    ): Unit =
        repo.findByName(connectionName).let { persistedConnection ->
            if (persistedConnection == null) throw ConnectionNotFoundException(connectionName)
            // else: save the connection back - keep ID only, take the rest of properties from the request
            repo.save(connection.copy(id = persistedConnection.id))
            // no need to do this in a transaction - in case of concurrent requests, the result is consistent state
            // make sure there isn't any obsolete cached connection hanging around
            manager.invalidateConnection(connectionName)
        }

    @Operation(summary = "Tests whether the connection can be established")
    @PostMapping("/connection/{connectionName}/test")
    @ResponseStatus(HttpStatus.OK)
    fun testConnection(
        @PathVariable connectionName: String
    ) {
        // try to list the schemas
        manager.listSchemas(connectionName)
        // if we didn't get exception, it's OK then
    }

    @Operation(summary = "List all connections")
    @GetMapping("/connection", produces = ["application/json"])
    fun listConnections(): List<ConnectionDetails> =
        repo.findAll()

    // we are returning HTTP 200 OK also on missing connection
    @Operation(summary = "Delete connection details")
    @DeleteMapping("/connection/{connectionName}")
    @Transactional
    fun deleteConnection(
        @PathVariable connectionName: String
    ) {
        repo.deleteByName(connectionName)
        manager.invalidateConnection(connectionName)
    }

}