package eu.kryl.ataccama.task.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.cache.RemovalNotification
import eu.kryl.ataccama.task.exception.ConnectionNotFoundException
import eu.kryl.ataccama.task.exception.SchemaNotFoundException
import eu.kryl.ataccama.task.exception.SqlExecutionException
import eu.kryl.ataccama.task.exception.TableNotFoundException
import eu.kryl.ataccama.task.model.ColumnEntry
import eu.kryl.ataccama.task.model.ConnectionDetails
import eu.kryl.ataccama.task.model.KeyType.PRI
import eu.kryl.ataccama.task.model.SchemaEntry
import eu.kryl.ataccama.task.model.TableEntry
import eu.kryl.ataccama.task.repository.ConnectionDetailsRepository
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import javax.sql.DataSource

/**
 * Manages list of live cached database connections.
 * Executes pre-configured queries upon those connections.
 */
@Component
class DatabaseIntrospectionService(
    val repository: ConnectionDetailsRepository
) {

    companion object : Logging {

        /**
         * How long is the connection kept alive after last access.
         */
        const val CONNECTION_CLOSE_TIMEOUT_MINUTES = 2

        val numericDataTypes =
            setOf("bigint", "tinyint", "smallint", "mediumint", "int", "integer", "decimal", "dec", "numeric", "fixed", "float", "double")

    }

    // let's have our mappers
    val schemaEntryMapper: BeanPropertyRowMapper<SchemaEntry> by lazy { BeanPropertyRowMapper.newInstance(SchemaEntry::class.java) }
    val tableEntryMapper: BeanPropertyRowMapper<TableEntry> by lazy { BeanPropertyRowMapper.newInstance(TableEntry::class.java) }
    val columnEntryMapper: BeanPropertyRowMapper<ColumnEntry> by lazy { BeanPropertyRowMapper.newInstance(ColumnEntry::class.java) }

    // cache of live named data sources
    private val dataSourceCache: LoadingCache<String, Pair<DataSource, SqlQueryExecutor>> = CacheBuilder
        .newBuilder()
        .removalListener { it: RemovalNotification<String, Pair<DataSource, SqlQueryExecutor>> ->
            // shutdown the connection when the DataSource is removed from the cache
            try {
                logger.info("closing connection to ${it.key}")
                it.value.first.connection.close()
            } catch (e: Throwable) {
                // we might safely ignore the exception
                logger.info("error occurred during connection shutdown [ignored]", e)
            }
        }
        .expireAfterAccess(Duration.ofMinutes(CONNECTION_CLOSE_TIMEOUT_MINUTES.toLong()))
        .build(
            CacheLoader.from { connectionName ->
                // look up the datasource
                val dbConnection = repository.findByName(connectionName!!)
                dbConnection?.let { dbc ->
                    logger.info("opening connection to $connectionName")
                    createDataSource(dbc).let { Pair(it, SqlQueryExecutor(JdbcTemplate(it, false))) }
                } ?: throw ConnectionNotFoundException(connectionName)
            }
        )

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // public API
    //

    fun invalidateConnection(connectionName: String) {
        dataSourceCache.invalidate(connectionName)
    }

    fun listSchemas(connectionName: String): List<SchemaEntry> =
        executeQuery(connectionName) { q ->
            q.query(
                "select schema_name as schemaName," +
                        "CATALOG_NAME as catalogName," +
                        "DEFAULT_CHARACTER_SET_NAME as defaultCharacterSetName," +
                        "DEFAULT_COLLATION_NAME as defaultCollationName," +
                        "SQL_PATH as sqlPath," +
                        "SCHEMA_COMMENT as schemaComment" +
                        " from INFORMATION_SCHEMA.SCHEMATA order by SCHEMA_NAME",
                schemaEntryMapper
            )
        }

    fun listTables(connectionName: String, schemaName: String): List<TableEntry> =
        // first check schema existence
        checkSchemaExistence(connectionName, schemaName).let {
            executeQuery(connectionName) { q ->
                q.query(
                    "select table_name as name," +
                            "ENGINE," +
                            "VERSION," +
                            "table_rows as tableRows," +
                            "avg_row_length as avgRowLength," +
                            "data_length as dataLength, " +
                            "index_length as indexLength," +
                            "create_time as createTime," +
                            "update_time as updateTime," +
                            "table_collation as tableCollation" +
                            // the assignment says 'tables only'
                            " from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA = ? and TABLE_TYPE = 'BASE TABLE'",
                    arrayOf(schemaName),
                    tableEntryMapper
                )
            }
        }

    fun listColumnStatistics(connectionName: String, schemaName: String, tableName: String): Map<String, Number?> =
        // checking also table existence
        listColumns(connectionName, schemaName, tableName).let { allColumns ->
            // retrieve columns we are interested in
            val numericColumns = allColumns.filter {
                numericDataTypes.contains(it.dataType) && it.columnKey == null   // we are not interested in key columns
            }
            val primaryKeyColumns = allColumns.filter { it.columnKey == PRI }.joinToString(separator = ",") { it.name }
            if (numericColumns.isEmpty()) {
                // there are no numeric columns
                emptyMap()
            } else {
                @Suppress("UNCHECKED_CAST")
                executeQuery(connectionName) { q ->
                    q.queryForMap(
                        numericColumns.joinToString(", ", "select ", " from $tableName") { buildColumnStatsQuery(it.name, primaryKeyColumns) }
                    ) as Map<String, Number?>
                }
            }

        }

    fun listColumns(connectionName: String, schemaName: String, tableName: String): List<ColumnEntry> {
        // first check table existence
        checkTableExistence(connectionName, schemaName, tableName)
        return executeQuery(connectionName) { q ->
            q.query(
                "select column_name as name," +
                        "ORDINAL_POSITION as position," +
                        "COLUMN_DEFAULT as defaultValue," +
                        "(IS_NULLABLE = 'YES') as nullable," +
                        "COLUMN_TYPE as columnType," +
                        "DATA_TYPE as dataType," +
                        "CHARACTER_MAXIMUM_LENGTH as characterMaxLength," +
                        "NUMERIC_PRECISION as numericPrecision," +
                        "NUMERIC_SCALE as numericScale," +
                        "DATETIME_PRECISION as datetimePrecision," +
                        "CHARACTER_SET_NAME as characterSetName," +
                        "COLLATION_NAME as collationName," +
                        "COLUMN_KEY as columnKey," +
                        "EXTRA as extra," +
                        "PRIVILEGES as privileges" +
                        " from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA = ? and TABLE_NAME = ?",
                arrayOf(schemaName, tableName),
                columnEntryMapper
            )
        }
    }


    fun previewData(connectionName: String, schemaName: String, tableName: String, pageable: Pageable): List<Map<String, Any?>> {
        // first check table existence
        checkTableExistence(connectionName, schemaName, tableName)
        // we have validated table existence, no worries about SQL injection
        return executeQuery(connectionName) { q ->
            q.queryForList("select * from $tableName limit ${pageable.pageSize} offset ${pageable.offset}")
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // helper internal functions
    //

    @Scheduled(fixedRate = 10000)
    private fun checkCachedConnections() {
        // cleanup expired connections each 10 seconds
        dataSourceCache.cleanUp()
    }

    private fun checkSchemaExistence(connectionName: String, schemaName: String) {
        executeQuery(connectionName) { q ->
            val s = q.queryForObject(
                "select count(*) as schemaCount from INFORMATION_SCHEMA.SCHEMATA where SCHEMA_NAME = ?",
                arrayOf(schemaName),
                Integer::class.java
            )
            if (s < 1) {
                throw SchemaNotFoundException(schemaName, connectionName)
            } else if (s > 1) {
                throw IllegalStateException("multiple schemas with the same name?! FIXME")
            }
            s
        }
    }

    private fun checkTableExistence(connectionName: String, schemaName: String, tableName: String) {
        executeQuery(connectionName) { q ->
            val r = q.queryForObject(
                "select count(*) from INFORMATION_SCHEMA.TABLES where TABLE_NAME = ? and TABLE_SCHEMA = ? and TABLE_TYPE = 'BASE TABLE'",
                arrayOf(tableName, schemaName),
                Integer::class.java
            )
            if (r < 1) {
                throw TableNotFoundException(tableName, schemaName)
            } else if (r > 1) {
                throw IllegalStateException("multiple tables with the same name?! FIXME")
            }
        }
    }

    private fun createDataSource(dbc: ConnectionDetails): DataSource = DataSourceBuilder.create()
        .driverClassName("org.mariadb.jdbc.Driver")
        .url("jdbc:mariadb://${dbc.hostname}:${dbc.port}/${dbc.databaseName}")
        .username(dbc.username)
        .password(dbc.password)
        .build()


    private fun buildColumnStatsQuery(columnName: String, idColumnNamesCommaSeparated: String) =
        columnName.let { c ->
            "min($c) as ${c}_min, max($c) as ${c}_max, avg($c) as ${c}_avg, median($c) over (partition by ${idColumnNamesCommaSeparated}) as ${c}_median"
        }


    private fun <T> executeQuery(connectionName: String, queryConsumer: (JdbcTemplate) -> T): T =
        dataSourceCache.get(connectionName).second.execute(queryConsumer)


    // we will create a wrapper around JdbcTemplate catching the raised exception
    inner class SqlQueryExecutor(private val delegate: JdbcTemplate) {

        fun <T> execute(queryConsumer: (JdbcTemplate) -> T): T {
            // uniform exception handling
            try {
                return queryConsumer(delegate)
            } catch (t: Throwable) {
                logger.info("exception occurred while executing query", t)
                throw SqlExecutionException(t)
            }
        }

    }

}

