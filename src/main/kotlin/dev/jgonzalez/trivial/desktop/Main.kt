package dev.jgonzalez.trivial.desktop

import dev.jgonzalez.trivial.desktop.GameStats
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.jgonzalez.trivial.desktop.network.TriviaClient
import dev.jgonzalez.trivial.shared.domain.Category
import dev.jgonzalez.trivial.shared.domain.Difficulty
import dev.jgonzalez.trivial.shared.domain.GameMode
import dev.jgonzalez.trivial.shared.domain.TurnMode
import kotlinx.coroutines.delay

fun main() = application {

    val client = remember { TriviaClient() }
    val lastMessage by client.lastMessage.collectAsState()
    val currentQuestion by client.currentQuestion.collectAsState()
    val answerResult by client.answerResult.collectAsState()
    val score by client.score.collectAsState()
    val gameEnd by client.gameEnd.collectAsState()
    val serverTimeLimit by client.timeLimit.collectAsState()

    var questionCount by remember { mutableStateOf(5) }
    var selectedDifficulty by remember { mutableStateOf(Difficulty.MIXED) }
    var selectedMode by remember { mutableStateOf(GameMode.PVE) }
    val isGameRunning = currentQuestion != null && gameEnd == null
    val scrollState = rememberScrollState()
    var timeLimit by remember { mutableStateOf(0) }
    var remainingTime by remember { mutableStateOf<Int?>(null) }
    var playerName by remember { mutableStateOf("Player1") }
    var selectedCategories by remember {
        mutableStateOf(listOf(Category.SCIENCE, Category.HISTORY))
    }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }

    // estadísticas
    var correctInCurrentGame by remember { mutableStateOf(0) }
    var stats by remember { mutableStateOf(StatsStorage.loadStats()) }
    var hasAppliedEndStats by remember { mutableStateOf(false) }

    // PVP
    var currentPlayerIndex by remember { mutableStateOf(0) } // 0 = Player1, 1 = Player2
    val playerNames = listOf("Player1", "Player2")
    var player1Score by remember { mutableStateOf(0) }
    var player2Score by remember { mutableStateOf(0) }
    var player1Streak by remember { mutableStateOf(0) }
    var player2Streak by remember { mutableStateOf(0) }
    var player1Correct by remember { mutableStateOf(0) }
    var player2Correct by remember { mutableStateOf(0) }

    // efecto: cuando llega una nueva AnswerResult, actualizar contadores UNA sola vez
    LaunchedEffect(answerResult) {
        val r = answerResult ?: return@LaunchedEffect
        val q = currentQuestion

        if (r.correct) {
            correctInCurrentGame++
        }

        if (q != null) {
            val cat = q.category
            val diff = selectedDifficulty

            val newTotalByCat = stats.totalByCategory.toMutableMap()
            newTotalByCat[cat] = (newTotalByCat[cat] ?: 0) + 1

            val newCorrectByCat = stats.correctByCategory.toMutableMap()
            if (r.correct) {
                newCorrectByCat[cat] = (newCorrectByCat[cat] ?: 0) + 1
            }

            val newTotalByDiff = stats.totalByDifficulty.toMutableMap()
            newTotalByDiff[diff] = (newTotalByDiff[diff] ?: 0) + 1

            val newCorrectByDiff = stats.correctByDifficulty.toMutableMap()
            if (r.correct) {
                newCorrectByDiff[diff] = (newCorrectByDiff[diff] ?: 0) + 1
            }

            stats = stats.copy(
                totalByCategory = newTotalByCat,
                correctByCategory = newCorrectByCat,
                totalByDifficulty = newTotalByDiff,
                correctByDifficulty = newCorrectByDiff
            )
            StatsStorage.saveStats(stats)
        }

        // NUEVO: actualizar PVP y cambiar turno
        if (selectedMode == GameMode.PVP) {
            if (currentPlayerIndex == 0) {
                if (r.correct) {
                    player1Correct++
                    player1Streak++
                } else {
                    player1Streak = 0
                }
                player1Score += r.points
            } else {
                if (r.correct) {
                    player2Correct++
                    player2Streak++
                } else {
                    player2Streak = 0
                }
                player2Score += r.points
            }
            // cambiar de jugador para la siguiente pregunta
            currentPlayerIndex = 1 - currentPlayerIndex
            playerName = playerNames[currentPlayerIndex]
        }
    }


    Window(
        onCloseRequest = ::exitApplication,
        title = "Trivial Multijugador"
    ) {

        MaterialTheme {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                Text("Cliente Trivial (PVE/PVP)")
                Spacer(Modifier.height(16.dp))

                // Modo de juego
                Text("Modo de juego")
                Row {
                    Button(
                        onClick = { selectedMode = GameMode.PVE },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMode == GameMode.PVE) Color(0xFF6750A4) else Color.LightGray
                        )
                    ) { Text("PVE") }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = { selectedMode = GameMode.PVP },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMode == GameMode.PVP) Color(0xFF6750A4) else Color.LightGray
                        )
                    ) { Text("PVP") }
                }

                if (selectedMode == GameMode.PVP) {
                    Text("Modo PVP: Player1 vs Player2 (turnos)")
                } else {
                    Text("Nombre jugador: $playerName")
                }

                Row {
                    Button(
                        onClick = { playerName = "Player1" },
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("Player1") }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = { playerName = "Player2" },
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("Player2") }
                }

                Spacer(Modifier.height(16.dp))

                // Número de preguntas
                Text("Número de preguntas: $questionCount")
                Slider(
                    value = questionCount.toFloat(),
                    onValueChange = { questionCount = (it.toInt() / 5) * 5 },
                    valueRange = 5f..20f,
                    steps = 2   // 5–10–15–20
                )

                Spacer(Modifier.height(16.dp))

                // Dificultad
                Text("Dificultad")
                Row {
                    val difficulties = listOf(
                        Difficulty.EASY to "Fácil",
                        Difficulty.MEDIUM to "Media",
                        Difficulty.HARD to "Difícil"
                    )
                    difficulties.forEach { (difficulty, label) ->
                        Button(
                            onClick = { selectedDifficulty = difficulty },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedDifficulty == difficulty) Color(0xFF6750A4) else Color.LightGray
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) { Text(label) }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Categorías
                Text("Categorías")

                val allCategories = listOf(
                    Category.SCIENCE to "Ciencia",
                    Category.HISTORY to "Historia",
                    Category.GEOGRAPHY to "Geografía",
                    Category.ART to "Arte",
                    Category.ART_LITERATURE to "Arte y literatura",
                    Category.ENTERTAINMENT to "Entretenimiento",
                    Category.TECHNOLOGY to "Tecnología",
                    Category.SPORTS to "Deportes",
                    Category.GENERAL to "General"
                )

                Column {
                    Row {
                        allCategories.take(5).forEach { (category, label) ->
                            val isSelected = category in selectedCategories
                            Button(
                                onClick = {
                                    selectedCategories =
                                        if (isSelected) selectedCategories - category
                                        else selectedCategories + category
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF6750A4) else Color.LightGray
                                ),
                                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                            ) {
                                Text(label)
                            }
                        }
                    }
                    Row {
                        allCategories.drop(5).forEach { (category, label) ->
                            val isSelected = category in selectedCategories
                            Button(
                                onClick = {
                                    selectedCategories =
                                        if (isSelected) selectedCategories - category
                                        else selectedCategories + category
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF6750A4) else Color.LightGray
                                ),
                                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                            ) {
                                Text(label)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Tiempo por pregunta
                Text("Tiempo por pregunta: " + if (timeLimit == 0) "sin límite" else "$timeLimit s")
                Slider(
                    value = timeLimit.toFloat(),
                    onValueChange = { timeLimit = it.toInt() },
                    valueRange = 0f..30f,
                    steps = 5
                )

                // Botón conectar
                Button(
                    onClick = { client.connect() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Conectar al servidor")
                }

                // Botón empezar partida
                Button(
                    onClick = {
                        client.resetState()
                        selectedOptionIndex = null
                        correctInCurrentGame = 0
                        hasAppliedEndStats = false

                        currentPlayerIndex = 0
                        playerName = playerNames[currentPlayerIndex]
                        player1Score = 0
                        player2Score = 0
                        player1Streak = 0
                        player2Streak = 0
                        player1Correct = 0
                        player2Correct = 0

                        client.createTrivia(
                            mode = selectedMode,
                            questions = questionCount,
                            categories = selectedCategories,
                            difficulty = selectedDifficulty,
                            timeLimit = timeLimit,
                            turnMode = TurnMode.TIMED,
                            playerName = playerName
                        )
                    },
                    enabled = !isGameRunning,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Empezar partida")
                }

                // Zona de pregunta
                currentQuestion?.let { q ->

                    // Cada vez que cambia de pregunta, limpiamos selección local
                    LaunchedEffect(q.id) {
                        selectedOptionIndex = null
                    }

                    // Sincronizar remainingTime con el límite del servidor
                    LaunchedEffect(q.id, serverTimeLimit) {
                        remainingTime = serverTimeLimit
                        if (serverTimeLimit != null && serverTimeLimit!! > 0) {
                            var t = serverTimeLimit!!
                            while (t > 0 && currentQuestion != null) {
                                delay(1000)
                                t--
                                remainingTime = t
                            }
                        }
                    }

                    // Número de pregunta
                    Text(
                        text = "Pregunta ${q.index} / ${q.total}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    // Categoría + icono sencillo
                    Text(
                        text = "Categoría: ${q.category.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = when (q.category) {
                            Category.SCIENCE -> "🔬"
                            Category.HISTORY -> "📜"
                            Category.GEOGRAPHY -> "🌍"
                            Category.ART, Category.ART_LITERATURE -> "🎨"
                            Category.ENTERTAINMENT -> "🎬"
                            Category.TECHNOLOGY -> "💻"
                            Category.SPORTS -> "🏅"
                            Category.GENERAL -> "❓"
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // Barra de tiempo restante
                    remainingTime?.let { t ->
                        val total = serverTimeLimit ?: t
                        if (total > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text("Tiempo restante: ${t}s")
                            Slider(
                                value = t.toFloat(),
                                onValueChange = {},
                                valueRange = 0f..total.toFloat(),
                                enabled = false
                            )
                        }
                    }

                    // Enunciado
                    Text(
                        text = q.text,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    // Opciones
                    q.options.forEachIndexed { index, option ->

                        val isSelected = selectedOptionIndex == index
                        val isCorrectAnswer =
                            answerResult?.correct == true && isSelected

                        val isWrongSelected =
                            answerResult?.correct == false && isSelected

                        val backgroundColor = when {
                            isCorrectAnswer -> Color(0xFF4CAF50) // verde
                            isWrongSelected -> Color(0xFFF44336) // rojo
                            else -> Color(0xFF6750A4)          // morado base
                        }

                        Button(
                            onClick = {
                                if (answerResult == null) {
                                    selectedOptionIndex = index
                                    client.answerQuestion(q.id, index)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = backgroundColor
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(option)
                        }
                    }
                }

                // Marcador
                if (selectedMode == GameMode.PVP) {
                    Text(
                        text = "Turno de: ${playerNames[currentPlayerIndex]}",
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Text(
                        text = "Player1 -> Puntos: $player1Score | Racha: $player1Streak | Correctas: $player1Correct",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Player2 -> Puntos: $player2Score | Racha: $player2Streak | Correctas: $player2Correct",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    // Modo PVE
                    score?.let { s ->
                        val streakColor = if (s.streak >= 5) Color(0xFFFFC107) else Color.Unspecified
                        val streakText = if (s.streak >= 5) "Racha: ${s.streak} (x2!)" else "Racha: ${s.streak}"

                        Text(
                            text = "Puntos: ${s.score}",
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        Text(
                            text = streakText,
                            color = streakColor,
                            style = if (s.streak >= 5) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                }

                // Resultado de la última respuesta + explicación (solo UI)
                answerResult?.let { r ->
                    val text = if (r.correct) {
                        "¡Correcto! +${r.points} puntos\n${r.explanation}"
                    } else {
                        "Incorrecto. +${r.points} puntos\n${r.explanation}"
                    }
                    Text(
                        text = text,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Fin de partida: actualizar stats UNA sola vez
                gameEnd?.let { g ->
                    if (!hasAppliedEndStats) {
                        val finalScore = score?.score ?: 0
                        val finalStreak = score?.streak ?: 0
                        val finalCorrect = correctInCurrentGame

                        val newBestByPlayer = stats.bestScoreByPlayer.toMutableMap()
                        val currentBest = newBestByPlayer[playerName] ?: 0
                        if (finalScore > currentBest) {
                            newBestByPlayer[playerName] = finalScore
                        }

                        val newStats = stats.copy(
                            maxScore = maxOf(stats.maxScore, finalScore),
                            longestStreak = maxOf(stats.longestStreak, finalStreak),
                            totalCorrectAnswers = stats.totalCorrectAnswers + finalCorrect,
                            gamesPlayed = stats.gamesPlayed + 1,
                            bestScoreByPlayer = newBestByPlayer
                        )

                        stats = newStats
                        StatsStorage.saveStats(newStats)
                        hasAppliedEndStats = true
                    }
                    if (selectedMode == GameMode.PVP) {
                        Text(
                            text = "Ganador: ${if (player1Score > player2Score) "Player1" else if (player2Score > player1Score) "Player2" else "Empate"}",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "Marcador final -> Player1: $player1Score pts, Player2: $player2Score pts",
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        Text(
                            text = "Partida terminada. Puntuación final: ${score?.score ?: 0}",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Button(
                        onClick = {
                            client.resetState()
                            selectedOptionIndex = null
                            correctInCurrentGame = 0
                            hasAppliedEndStats = false
                            client.createTrivia(
                                mode = selectedMode,
                                questions = questionCount,
                                categories = selectedCategories,
                                difficulty = selectedDifficulty,
                                timeLimit = timeLimit,
                                turnMode = TurnMode.TIMED,
                                playerName = "Player1"
                            )
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Jugar otra vez")
                    }
                }

                Text(
                    text = "=== Estadísticas globales ===",
                    modifier = Modifier.padding(top = 16.dp)
                )
                Button(
                    onClick = {
                        stats = GameStats()           // todo a cero
                        StatsStorage.saveStats(stats)
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Reiniciar estadísticas")
                }



                Text(
                    text = "Partidas jugadas: ${stats.gamesPlayed}",
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "Máxima puntuación: ${stats.maxScore}",
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "Racha más larga: ${stats.longestStreak}",
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "Preguntas correctas totales: ${stats.totalCorrectAnswers}",
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Último mensaje JSON
                Text(
                    text = "Último mensaje recibido: ${lastMessage ?: "(ninguno)"}",
                    maxLines = 1,
                    modifier = Modifier.padding(top = 16.dp)
                )

                // Porcentaje por categoría
                Text(
                    text = "Porcentaje de aciertos por categoría:",
                    modifier = Modifier.padding(top = 8.dp)
                )
                stats.totalByCategory.forEach { (cat, total) ->
                    val correct = stats.correctByCategory[cat] ?: 0
                    val percent = if (total > 0) (correct * 100) / total else 0
                    Text(
                        text = "- ${cat.name}: $correct / $total ($percent%)",
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }

                // Porcentaje por dificultad
                Text(
                    text = "Porcentaje de aciertos por dificultad:",
                    modifier = Modifier.padding(top = 8.dp)
                )
                stats.totalByDifficulty.forEach { (diff, total) ->
                    val correct = stats.correctByDifficulty[diff] ?: 0
                    val percent = if (total > 0) (correct * 100) / total else 0
                    Text(
                        text = "- ${diff.name}: $correct / $total ($percent%)",
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }

                // Ranking de mejores jugadores
                Text(
                    text = "Ranking de jugadores (mejor puntuación):",
                    modifier = Modifier.padding(top = 8.dp)
                )
                stats.bestScoreByPlayer
                    .entries
                    .sortedByDescending { it.value }
                    .forEach { (name, bestScore) ->
                        Text(
                            text = "- $name: $bestScore puntos",
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                        )
                    }
            }
        }
    }
}
