package dev.jgonzalez.trivial.desktop.network

import dev.jgonzalez.trivial.shared.domain.Category
import dev.jgonzalez.trivial.shared.domain.ClientMessage
import dev.jgonzalez.trivial.shared.domain.Difficulty
import dev.jgonzalez.trivial.shared.domain.GameMode
import dev.jgonzalez.trivial.shared.domain.ServerMessage
import dev.jgonzalez.trivial.shared.domain.TurnMode
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.ws
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class UiQuestion(
    val id: String,
    val text: String,
    val options: List<String>,
    val index: Int,
    val total: Int,
    val category: Category
)

data class UiAnswerResult(
    val correct: Boolean,
    val points: Int,
    val explanation: String
)

data class UiScore(
    val score: Int,
    val streak: Int
)

data class UiGameEnd(
    val winner: String?,
    val finalScore: Int,
    val correctAnswers: Int
)

private val json = Json { ignoreUnknownKeys = true }

class TriviaClient {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _lastMessage = MutableStateFlow<String?>(null)
    val lastMessage: StateFlow<String?> = _lastMessage

    private val _currentQuestion = MutableStateFlow<UiQuestion?>(null)
    val currentQuestion: StateFlow<UiQuestion?> = _currentQuestion

    private val _answerResult = MutableStateFlow<UiAnswerResult?>(null)
    val answerResult: StateFlow<UiAnswerResult?> = _answerResult

    private val _score = MutableStateFlow<UiScore?>(null)
    val score: StateFlow<UiScore?> = _score

    private val _gameEnd = MutableStateFlow<UiGameEnd?>(null)
    val gameEnd: StateFlow<UiGameEnd?> = _gameEnd

    private val _timeLimit = MutableStateFlow<Int?>(null)
    val timeLimit: StateFlow<Int?> = _timeLimit

    private var questionStartTimeNanos: Long? = null
    private var currentTimeLimitSeconds: Int? = null

    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    private var session: WebSocketSession? = null

    fun connect() {
        scope.launch {
            try {
                client.ws(
                    host = "localhost",
                    port = 8080,
                    path = "/trivia"
                ) {
                    println("CLIENTE: Conectado")
                    session = this

                    for (frame in incoming) {
                        val text = (frame as? Frame.Text)?.readText() ?: continue
                        println("CLIENTE: Recibido -> $text")
                        _lastMessage.value = text

                        val msg = json.decodeFromString(ServerMessage.serializer(), text)
                        when (msg) {
                            is ServerMessage.QuestionMsg -> {
                                _currentQuestion.value = UiQuestion(
                                    id = msg.id,
                                    text = msg.question,
                                    options = msg.options,
                                    index = msg.index,
                                    total = msg.total,
                                    category = msg.category
                                )
                                _answerResult.value = null

                                questionStartTimeNanos = System.nanoTime()
                                currentTimeLimitSeconds = msg.timeLimit
                                _timeLimit.value = msg.timeLimit
                            }

                            is ServerMessage.AnswerResultMsg -> {
                                _answerResult.value = UiAnswerResult(
                                    correct = msg.correct,
                                    points = msg.points,
                                    explanation = msg.explanation
                                )
                            }

                            is ServerMessage.ScoreUpdateMsg -> {
                                val p = msg.players.firstOrNull()
                                if (p != null) {
                                    _score.value = UiScore(
                                        score = p.score,
                                        streak = p.streak
                                    )
                                }
                            }

                            is ServerMessage.GameEndMsg -> {
                                val p = msg.finalScores.firstOrNull()
                                val correctMap = msg.correctAnswers[p?.name] ?: 0
                                _gameEnd.value = UiGameEnd(
                                    winner = msg.winner,
                                    finalScore = p?.score ?: 0,
                                    correctAnswers = correctMap
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _lastMessage.value = "Error: ${e.message}"
                session = null
            }
        }
    }

    fun createTrivia(
        mode: GameMode,
        questions: Int,
        categories: List<Category>,
        difficulty: Difficulty,
        timeLimit: Int,
        turnMode: TurnMode,
        playerName: String
    ) {
        scope.launch {
            val s = session ?: return@launch

            val msg = ClientMessage.CreateTrivia(
                mode = mode,
                questions = questions,
                categories = categories,
                difficulty = difficulty,
                timeLimit = timeLimit,
                turnMode = turnMode,
                playerName = playerName
            )

            val text = json.encodeToString(ClientMessage.serializer(), msg)
            println("CLIENTE: Enviando CreateTrivia -> $text")
            s.send(Frame.Text(text))
        }
    }

    fun answerQuestion(questionId: String, selectedIndex: Int) {
        scope.launch {
            val s = session ?: return@launch

            val start = questionStartTimeNanos
            val elapsedSeconds =
                if (start != null) ((System.nanoTime() - start) / 1_000_000_000L).toInt()
                else 0

            val msg = ClientMessage.Answer(
                questionId = questionId,
                selectedOption = selectedIndex,
                timeElapsed = elapsedSeconds
            )
            val text = json.encodeToString(ClientMessage.serializer(), msg)
            s.send(Frame.Text(text))
        }
    }

    fun resetState() {
        _currentQuestion.value = null
        _answerResult.value = null
        _score.value = null
        _gameEnd.value = null
        _lastMessage.value = null
        _timeLimit.value = null
        questionStartTimeNanos = null
        currentTimeLimitSeconds = null
    }
}
