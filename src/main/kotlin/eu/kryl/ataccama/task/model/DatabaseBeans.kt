package eu.kryl.ataccama.task.model

import eu.kryl.ataccama.task.annotation.DatabaseBean
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Schema/database properties")
@DatabaseBean
data class SchemaEntry(
    var schemaName: String,
    var catalogName: String,
    var defaultCharacterSetName: String,
    var defaultCollationName: String,
    var sqlPath: String?,
    var schemaComment: String
)

@Schema(description = "Table properties")
@DatabaseBean
data class TableEntry(
    var name: String,
    var engine: String,
    var version: Int,
    var tableRows: Int,
    var avgRowLength: Int,
    var dataLength: Int,
    var indexLength: Int,
    var createTime: LocalDateTime?,
    var updateTime: LocalDateTime?,
    var tableCollation: String
    // there can be more data to show...
)

@Schema(description = "Column properties")
@DatabaseBean
data class ColumnEntry(
    var name: String,
    var dataType: String,
    var columnType: String,
    var position: Int,
    var defaultValue: String?,
    var nullable: Boolean,
    var characterMaxLengh: Int?,
    var numericPrecision: Int?,
    var numericScale: Int?,
    var datetimePrecision: Int?,
    var characterSetName: String?,
    var collationName: String?,
    var columnKey: KeyType?,
    var extra: String,
    var privileges: String
)


@Schema(description = "Key type: PRI - primary, UNI - unique, MUL - other (foreign key)")
enum class KeyType {
    PRI, UNI, MUL
}
