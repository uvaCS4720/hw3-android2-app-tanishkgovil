package edu.nd.pmcburne.hwapp.one

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Game::class], version = 2, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}


