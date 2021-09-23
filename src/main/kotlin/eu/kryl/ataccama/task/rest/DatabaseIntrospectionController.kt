package eu.kryl.ataccama.task.rest

import eu.kryl.ataccama.task.model.ColumnEntry
import eu.kryl.ataccama.task.model.SchemaEntry
import eu.kryl.ataccama.task.model.TableEntry
import eu.kryl.ataccama.task.service.DatabaseIntrospectionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * This controller delegates to work upon introspection service.
 *
 * @see DatabaseIntrospectionService
 */
@RestController
@Tags(Tag(name = "Database Introspection"))
class DatabaseIntrospectionController(
    private val service: DatabaseIntrospectionService,
) {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // database introspection
    //

    @Operation(summary = "Return schemas/databases of the given connection")
    @GetMapping("/connection/{connectionName}/schemas", produces = ["application/json"])
    fun getSchemas(
        @PathVariable connectionName: String
    ): List<SchemaEntry> =
        service.listSchemas(connectionName)


    @Operation(summary = "Return tables of the given schemas/databases behind the connection")
    @GetMapping("/connection/{connectionName}/schemas/{schemaName}/tables", produces = ["application/json"])
    fun getTables(
        @PathVariable connectionName: String,
        @PathVariable schemaName: String
    ): List<TableEntry> =
        service.listTables(connectionName, schemaName)


    @Operation(summary = "Return columns of the given table within database/schema behind the connection")
    @GetMapping("/connection/{connectionName}/schemas/{schemaName}/tables/{tableName}/columns", produces = ["application/json"])
    fun getColumns(
        @PathVariable connectionName: String,
        @PathVariable schemaName: String,
        @PathVariable tableName: String
    ): List<ColumnEntry> =
        service.listColumns(connectionName, schemaName, tableName)

    @Operation(summary = "Return data preview of the given table within database/schema behind the connection")
    @GetMapping("/connection/{connectionName}/schemas/{schemaName}/tables/{tableName}/data", produces = ["application/json"])
    fun previewData(
        @PathVariable connectionName: String,
        @PathVariable schemaName: String,
        @PathVariable tableName: String,
        @Parameter(
            required = false,
            description = "zero-based page number - pagination support"
        )
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @Parameter(
            required = false,
            description = "non-zero positive integer: size of page - pagination support"
        )
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): List<Map<String, Any?>> =
        service.previewData(connectionName, schemaName, tableName, PageRequest.of(page, size))

    @Operation(summary = "Return statistics about numeric columns of the given table within database/schema behind the connection")
    @GetMapping("/connection/{connectionName}/schemas/{schemaName}/tables/{tableName}/stats", produces = ["application/json"])
    fun getStatistics(
        @PathVariable connectionName: String,
        @PathVariable schemaName: String,
        @PathVariable tableName: String
    ): Map<String, Number?> =
        service.listColumnStatistics(connectionName, schemaName, tableName)

}

