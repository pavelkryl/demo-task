package eu.kryl.ataccama.task.annotation

import kotlin.annotation.AnnotationTarget.CLASS

/**
 * This annotation is configured for no-arg plugin to generate no-arg/default constructor.
 */
@Target(CLASS)
annotation class DatabaseBean
