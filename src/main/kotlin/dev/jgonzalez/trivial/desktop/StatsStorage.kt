package dev.jgonzalez.trivial.desktop

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object StatsStorage {

    private val json = Json { prettyPrint = true }
    private val file: File by lazy {
        val dir = File(System.getProperty("user.home"), ".trivial-multijugador")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        File(dir, "stats.json")
    }

    fun loadStats(): GameStats {
        return try {
            if (!file.exists()) {
                GameStats()
            } else {
                val text = file.readText()
                json.decodeFromString<GameStats>(text)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            GameStats()
        }
    }

    fun saveStats(stats: GameStats) {
        try {
            val text = json.encodeToString(stats)
            file.writeText(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
