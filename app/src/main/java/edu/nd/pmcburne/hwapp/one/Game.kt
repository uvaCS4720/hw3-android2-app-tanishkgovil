package edu.nd.pmcburne.hwapp.one

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class Game(
    @PrimaryKey val id: String,
    val date: String,
    val gender: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: String,
    val awayScore: String,
    val statusName: String,
    val displayClock: String,
    val period: Int,
    val startDate: String,
    val homeWinner: Boolean,
    val awayWinner: Boolean,
    val currentPeriod: String
)


