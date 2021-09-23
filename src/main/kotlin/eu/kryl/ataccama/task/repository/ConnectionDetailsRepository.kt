package eu.kryl.ataccama.task.repository

import eu.kryl.ataccama.task.model.ConnectionDetails
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Simple declarative approach.
 */
interface ConnectionDetailsRepository : JpaRepository<ConnectionDetails, Long> {

    fun findByName(name: String): ConnectionDetails?

    fun deleteByName(name: String)

}