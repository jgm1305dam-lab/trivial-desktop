package dev.jgonzalez.trivial.desktop

import dev.jgonzalez.trivial.shared.domain.Category
import dev.jgonzalez.trivial.shared.domain.Difficulty
import kotlinx.serialization.Serializable

@Serializable
data class GameStats(
    val maxScore: Int = 0,
    val longestStreak: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val gamesPlayed: Int = 0,
    val correctByCategory: Map<Category, Int> = emptyMap(),
    val totalByCategory: Map<Category, Int> = emptyMap(),
    val correctByDifficulty: Map<Difficulty, Int> = emptyMap(),
    val totalByDifficulty: Map<Difficulty, Int> = emptyMap(),
    val bestScoreByPlayer: Map<String, Int> = emptyMap()
)
