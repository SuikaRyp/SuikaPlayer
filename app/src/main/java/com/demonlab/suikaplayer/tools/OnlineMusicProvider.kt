package com.demonlab.suikaplayer.tools

import android.content.Context
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
class OnlineMusicProvider(private val context: Context) {

    companion object {
        // Large offset so online song ids never collide with local MediaStore ids.
        private const val ID_OFFSET = 1_000_000_000_000L
        private const val ASSET_NAME = "online_songs.json"
    }

    private var cachedSongs: List<Song>? = null

    suspend fun getOnlineSongs(forceReload: Boolean = false): List<Song> = withContext(Dispatchers.IO) {
        if (!forceReload) {
            cachedSongs?.let { return@withContext it }
        }

        val songs = try {
            val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            parseSongs(json)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }

        cachedSongs = songs
        songs
    }

    private fun parseSongs(json: String): List<Song> {
        val array = JSONArray(json)
        val result = mutableListOf<Song>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val src = obj.optString("src").takeIf { it.isNotBlank() } ?: continue
            val rawId = obj.optLong("id", (i + 1).toLong())

            result.add(
                Song(
                    id = ID_OFFSET + rawId,
                    albumId = 0L,
                    title = obj.optString("title", "Unknown").ifBlank { "Unknown" },
                    artist = obj.optString("artist", "Unknown Artist").ifBlank { "Unknown Artist" },
                    album = obj.optString("album", "").ifBlank { "Online" },
                    duration = parseDurationToMillis(obj.optString("duration", "0:00")),
                    uri = src.toUri(),
                    path = src,
                    dateAdded = rawId,
                    albumArtUri = null,
                    genre = obj.optString("category").takeIf { it.isNotBlank() },
                    folderName = "Online",
                    isHiFi = false,
                    coverUrl = obj.optString("img").takeIf { it.isNotBlank() },
                    isFavorite = false,
                    lyrics = obj.optString("lyrics").takeIf { it.isNotBlank() },
                    format = "MP3",
                    bitrate = null,
                    trackNumber = 0
                )
            )
        }

        return result
    }

    private fun parseDurationToMillis(duration: String): Long {
        return try {
            val parts = duration.trim().split(":").map { it.toInt() }
            when (parts.size) {
                2 -> ((parts[0] * 60) + parts[1]) * 1000L
                3 -> ((parts[0] * 3600) + (parts[1] * 60) + parts[2]) * 1000L
                else -> 0L
            }
        } catch (e: Exception) {
            0L
        }
    }
}
