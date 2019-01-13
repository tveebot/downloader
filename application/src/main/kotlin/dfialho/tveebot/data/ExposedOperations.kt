package dfialho.tveebot.data

import dfialho.tveebot.utils.Result
import org.h2.api.ErrorCode
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.sql.SQLException

/**
 * Inserts a new element into a [Table].
 * The output of this method is as follows:
 * - Returns [Result.Success] if the insert operation is successful
 * - Returns [Result.Failure] if the insert operation fails because the table already includes the
 *   element being inserted (duplicate key error).
 * - Throws [ExposedSQLException] if some other error occurs besides a "duplicate key error".
 *
 * WARNING: At this point this operation is only supported for H2 databases. Please update it if we
 * need to support other SQL databases.
 */
fun <T : Table> T.insertUnique(body: T.(InsertStatement<Number>) -> Unit): Result {
    return try {
        insert(body)
        Result.Success

    } catch (e: ExposedSQLException) {
        val sqlException = e.cause as SQLException

        // WARNING: This is an hack that works only for H2
        if (sqlException.errorCode == ErrorCode.DUPLICATE_KEY_1) {
            Result.Failure
        } else {
            throw e
        }
    }
}