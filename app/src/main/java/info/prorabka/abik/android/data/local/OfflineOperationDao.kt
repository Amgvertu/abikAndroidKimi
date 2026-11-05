package info.prorabka.abik.android.data.local

import androidx.room.*

@Dao
interface OfflineOperationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(operation: OfflineOperationEntity)

    @Update
    suspend fun update(operation: OfflineOperationEntity)

    @Delete
    suspend fun delete(operation: OfflineOperationEntity)

    @Query("SELECT * FROM offline_operations WHERE userId = :userId AND status = 'PENDING' ORDER BY createdAt ASC LIMIT 1")
    suspend fun nextPending(userId: Long): OfflineOperationEntity?

    @Query("SELECT * FROM offline_operations WHERE userId = :userId AND teamId = :teamId AND type = :type ORDER BY createdAt ASC")
    suspend fun forType(userId: Long, teamId: Long, type: String): List<OfflineOperationEntity>

    @Query("SELECT * FROM offline_operations WHERE userId = :userId AND teamId = :teamId AND type IN (:types) ORDER BY createdAt ASC")
    suspend fun forTypes(userId: Long, teamId: Long, types: List<String>): List<OfflineOperationEntity>

    @Query("DELETE FROM offline_operations WHERE userId = :userId AND teamId = :teamId AND type = :type AND status = 'SYNCED'")
    suspend fun deleteSynced(userId: Long, teamId: Long, type: String)

    @Query("DELETE FROM offline_operations WHERE userId = :userId AND teamId = :teamId AND type = :type AND eventId = :eventId AND status = 'PENDING'")
    suspend fun deletePendingEvent(userId: Long, teamId: Long, type: String, eventId: Long)

    @Query("DELETE FROM offline_operations WHERE userId = :userId AND teamId = :teamId AND type = :type AND eventId = :eventId AND status = 'SYNCED'")
    suspend fun deleteSyncedEvent(userId: Long, teamId: Long, type: String, eventId: Long)

    @Query("DELETE FROM offline_operations WHERE userId = :userId AND teamId = :teamId AND type = :type AND eventId = :eventId")
    suspend fun deleteEventOperations(userId: Long, teamId: Long, type: String, eventId: Long)
}
