package com.racetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [UserEntity::class, SessionEntity::class, TrackPointEntity::class, VehicleEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun raceDao(): RaceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val MIGRATION_3_4 = object : Migration(3, 4) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE users ADD COLUMN photoUri TEXT")
                    }
                }

                val MIGRATION_4_5 = object : Migration(4, 5) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // Create vehicles table
                        database.execSQL("CREATE TABLE IF NOT EXISTS `vehicles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `model` TEXT NOT NULL, `photoUri` TEXT, FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                        database.execSQL("CREATE INDEX IF NOT EXISTS `index_vehicles_userId` ON `vehicles` (`userId`)")
                        
                        // Migrate existing vehicles from users to vehicles
                        database.execSQL("INSERT INTO `vehicles` (`userId`, `model`, `photoUri`) SELECT `id`, `vehicleModel`, `photoUri` FROM `users`")
                        
                        // Alter sessions to add vehicleId
                        database.execSQL("ALTER TABLE `sessions` ADD COLUMN `vehicleId` INTEGER")
                        
                        // Update existing sessions to link to the new vehicle
                        database.execSQL("UPDATE `sessions` SET `vehicleId` = (SELECT `id` FROM `vehicles` WHERE `vehicles`.`userId` = `sessions`.`userId` LIMIT 1)")
                    }
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "race_tracker_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
