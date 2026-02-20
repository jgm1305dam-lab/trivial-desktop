package dev.jgonzalez.trivial.shared.domain

import kotlinx.serialization.Serializable

// -------- CLIENT → SERVER --------

@Serializable
sealed class ClientMessage {

    @Serializable
    data class CreateTrivia(
        val mode: GameMode,
        val questions: Int,
        val categories: List<Category>,
        val difficulty: Difficulty,
        val timeLimit: Int?,         // segundos o null = sin límite
        val turnMode: TurnMode,
        val playerName: String? = null
    ) : ClientMessage()

    @Serializable
    data class Answer(
        val questionId: String,      // IMPORTANTE: String, no Int
        val selectedOption: Int,
        val timeElapsed: Int         // segundos
    ) : ClientMessage()
}

// -------- SERVER → CLIENT --------

@Serializable
data class PlayerScoreDto(
    val name: String,
    val score: Int,
    val streak: Int
)

@Serializable
sealed class ServerMessage {

    @Serializable
    data class QuestionMsg(
        val id: String,              // id de la pregunta
        val category: Category,
        val difficulty: Difficulty,
        val question: String,
        val options: List<String>,
        val timeLimit: Int?,         // límite para esta pregunta
        val index: Int,              // número de pregunta (1-based)
        val total: Int               // total de preguntas de la partida
    ) : ServerMessage()

    @Serializable
    data class AnswerResultMsg(
        val questionId: String,
        val correct: Boolean,
        val correctAnswer: Int,      // índice de la opción correcta
        val points: Int,
        val explanation: String
    ) : ServerMessage()

    @Serializable
    data class ScoreUpdateMsg(
        val players: List<PlayerScoreDto>
    ) : ServerMessage()

    @Serializable
    data class GameEndMsg(
        val winner: String?,
        val finalScores: List<PlayerScoreDto>,
        val correctAnswers: Map<String, Int>
    ) : ServerMessage()
}
