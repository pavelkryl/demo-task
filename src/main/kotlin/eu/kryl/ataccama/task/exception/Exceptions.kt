package eu.kryl.ataccama.task.exception

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

// extends ResponseStatusException so that we do not have to do exception translation

class ConnectionAlreadyExistsException(connectionName: String) :
    ResponseStatusException(HttpStatus.CONFLICT, "connection named $connectionName already exists")

class ConnectionNotFoundException(connectionName: String) :
    ResponseStatusException(HttpStatus.NOT_FOUND, "connection $connectionName not found")

class SchemaNotFoundException(schemaName: String, connectionName: String) :
    ResponseStatusException(HttpStatus.NOT_FOUND, "schema $schemaName not found within connection $connectionName")

class TableNotFoundException(tableName: String, schemaName: String) :
    ResponseStatusException(HttpStatus.NOT_FOUND, "table $tableName not found within schema $schemaName")

class SqlExecutionException(t: Throwable):
    ResponseStatusException(HttpStatus.FAILED_DEPENDENCY, t.message)