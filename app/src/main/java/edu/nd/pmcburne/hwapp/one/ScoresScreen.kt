package edu.nd.pmcburne.hwapp.one

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoresScreen(viewModel: ScoresViewModel) {
    val selectedDate = viewModel.selectedDate.collectAsState()
    val isMen = viewModel.isMen.collectAsState()
    val games = viewModel.games.collectAsState()
    val isLoading = viewModel.isLoading.collectAsState()
    val errorMessage = viewModel.errorMessage.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Basketball Scores") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { showDatePicker = true }) {
                        Text(
                            selectedDate.value.format(
                                DateTimeFormatter.ofPattern("MMM d, yyyy")
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Men's",
                            fontWeight = if (isMen.value) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = !isMen.value,
                            onCheckedChange = { viewModel.onGenderToggle() }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Women's",
                            fontWeight = if (!isMen.value) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Button(
                    onClick = { viewModel.loadGames() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Refresh")
                }

                errorMessage.value?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                if (games.value.isEmpty() && !isLoading.value) {
                    Text(
                        text = "No games found for this date.",
                        modifier = Modifier.padding(16.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(games.value) { game ->
                        GameCard(game = game)
                    }
                }
            }

            if (isLoading.value) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.value
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        viewModel.onDateChanged(newDate)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun GameCard(game: Game) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "(Away) ${game.awayTeam}",
                    fontWeight = if (game.awayWinner) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (game.statusName != "STATUS_SCHEDULED") {
                    Text(
                        text = game.awayScore,
                        fontWeight = if (game.awayWinner) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "(Home) ${game.homeTeam}",
                    fontWeight = if (game.homeWinner) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (game.statusName != "STATUS_SCHEDULED") {
                    Text(
                        text = game.homeScore,
                        fontWeight = if (game.homeWinner) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = getStatusText(game),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun getStatusText(game: Game): String {
    return when (game.statusName) {
        "STATUS_FINAL" -> {
            val winner = when {
                game.homeWinner -> game.homeTeam
                game.awayWinner -> game.awayTeam
                else -> null
            }
            if (winner != null) {
                "Final - Winner: $winner"
            } else {
                "Final"
            }
        }
        "STATUS_IN_PROGRESS" -> {
            val periodText = game.currentPeriod.ifEmpty { "In Progress" }
            "$periodText - ${game.displayClock} remaining"
        }
        else -> {
            "Scheduled - ${game.startDate}"
        }
    }
}
