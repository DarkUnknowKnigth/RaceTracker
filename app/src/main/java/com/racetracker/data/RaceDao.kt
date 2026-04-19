package com.racetracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RaceDao {
    @Insert
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserById(userId: Int): Flow<UserEntity?>

    @Update
    suspend fun updateUser(user: UserEntity)

    @Insert
    suspend fun insertVehicle(vehicle: VehicleEntity): Long

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Delete
    suspend fun deleteVehicle(vehicle: VehicleEntity)

    @Query("SELECT * FROM vehicles WHERE userId = :userId")
    fun getVehiclesForUser(userId: Int): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :vehicleId LIMIT 1")
    fun getVehicleById(vehicleId: Int): Flow<VehicleEntity?>

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE userId = :userId ORDER BY startTime DESC")
    fun getSessionsForUser(userId: Int): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionById(sessionId: Int): Flow<SessionEntity?>

    @Insert
    suspend fun insertTrackPoint(point: TrackPointEntity)

    @Query("SELECT * FROM track_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getTrackPointsForSession(sessionId: Int): Flow<List<TrackPointEntity>>
    
    @Query("SELECT MAX(speedKmh) FROM track_points WHERE sessionId = :sessionId")
    suspend fun getMaxSpeedForSession(sessionId: Int): Float?

    @Query("SELECT * FROM sessions WHERE userId = :userId ORDER BY startTime DESC LIMIT 1")
    fun getLastSessionForUser(userId: Int): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE userId = :userId AND endTime IS NULL LIMIT 1")
    suspend fun getActiveSessionForUser(userId: Int): SessionEntity?

    @Query("SELECT MAX(maxSpeed) FROM sessions WHERE userId = :userId")
    fun getOverallTopSpeedForUser(userId: Int): Flow<Float?>
    
    @Query("UPDATE sessions SET endTime = :endTime, maxSpeed = :maxSpeed, maxBrakingGForce = :maxBrakingGForce, distanceMeters = :distanceMeters WHERE id = :sessionId")
    suspend fun finalizeSession(sessionId: Int, endTime: Long, maxSpeed: Float, maxBrakingGForce: Float, distanceMeters: Float)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Int)

    @Query("DELETE FROM track_points WHERE sessionId = :sessionId")
    suspend fun deleteTrackPointsForSession(sessionId: Int)
}
