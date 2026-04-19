package com.racetracker.data

import androidx.room.*

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val passwordHash: String,
    val vehicleModel: String,
    val photoUri: String? = null
)

@Entity(
    tableName = "vehicles",
    indices = [Index("userId")],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val model: String,
    val photoUri: String? = null
)

@Entity(tableName = "sessions", foreignKeys = [
    ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )
])
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val startTime: Long,
    val endTime: Long? = null,
    val maxSpeed: Float = 0f,
    val maxBrakingGForce: Float = 0f,
    val distanceMeters: Float = 0f,
    val isSynced: Boolean = false,
    val vehicleId: Int? = null
)

@Entity(tableName = "track_points", foreignKeys = [
    ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )
])
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float,
    val acceleration: Float
)
