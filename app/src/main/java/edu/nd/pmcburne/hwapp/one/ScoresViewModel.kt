package edu.nd.pmcburne.hwapp.one

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class ScoresViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _isMen = MutableStateFlow(true)
    val isMen: StateFlow<Boolean> = _isMen.asStateFlow()

    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games: StateFlow<List<Game>> = _games.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val db = Room.databaseBuilder(
        application, GameDatabase::class.java, "basketball-scores-db"
    ).fallbackToDestructiveMigration(true).build()

    private val api = RetrofitClient.api

    init {
        loadGames()
    }

    fun loadGames() {
        viewModelScope.launch {
            _isLoading.update { true }
            _errorMessage.update { null }

            val gender = if (_isMen.value) "men" else "women"
            val date = _selectedDate.value
            val dateStr = date.toString()
            val year = String.format("%04d", date.year)
            val month = String.format("%02d", date.monthValue)
            val day = String.format("%02d", date.dayOfMonth)

            try {
                val response = api.getScoreboard(gender, year, month, day)
                val gamesList = parseGames(response, dateStr, gender)

                db.gameDao().deleteGames(dateStr, gender)
                db.gameDao().insertGames(gamesList)

                _games.update { gamesList }
            } catch (e: Exception) {
                val cached = db.gameDao().getGames(dateStr, gender)
                _games.update { cached }
                _errorMessage.update {
                    if (cached.isEmpty()) {
                        "No internet connection and no cached data"
                    } else {
                        "Offline - showing cached data"
                    }
                }
            }

            _isLoading.update { false }
        }
    }

    fun onDateChanged(newDate: LocalDate) {
        _selectedDate.update { newDate }
        loadGames()
    }

    fun onGenderToggle() {
        _isMen.update { oldValue -> !oldValue }
        loadGames()
    }

    private fun parseGames(
        response: ScoreboardResponse,
        dateStr: String,
        gender: String
    ): List<Game> {
        return response.games?.mapNotNull { wrapper ->
            val g = wrapper.game ?: return@mapNotNull null

            val statusName = when (g.gameState) {
                "final" -> "STATUS_FINAL"
                "live" -> "STATUS_IN_PROGRESS"
                else -> "STATUS_SCHEDULED"
            }

            Game(
                id = g.gameID ?: return@mapNotNull null,
                date = dateStr,
                gender = gender,
                homeTeam = g.home?.names?.short ?: "Unknown",
                awayTeam = g.away?.names?.short ?: "Unknown",
                homeScore = g.home?.score ?: "0",
                awayScore = g.away?.score ?: "0",
                statusName = statusName,
                displayClock = g.contestClock ?: "0:00",
                period = 0,
                startDate = g.startTime ?: "",
                homeWinner = g.home?.winner ?: false,
                awayWinner = g.away?.winner ?: false,
                currentPeriod = g.currentPeriod ?: ""
            )
        } ?: emptyList()
    }
}
