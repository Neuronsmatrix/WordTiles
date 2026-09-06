package com.wordtiles.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteException
import com.wordtiles.core.Progress
import com.wordtiles.core.WordEntry
import com.wordtiles.core.canonicalWord
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WordStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
) {
    override fun onCreate(database: SQLiteDatabase) {
        createTables(database)
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Every migration is additive so downloaded entries and learning history survive upgrades.
        createTables(database)
    }

    fun entries(): Map<String, WordEntry> {
        val result = linkedMapOf<String, WordEntry>()
        readableDatabase.query(
            ENTRIES_TABLE,
            arrayOf(WORD_COLUMN, PAYLOAD_COLUMN),
            null,
            null,
            null,
            null,
            "$WORD_COLUMN ASC",
        ).use { cursor ->
            val wordIndex = cursor.getColumnIndexOrThrow(WORD_COLUMN)
            val payloadIndex = cursor.getColumnIndexOrThrow(PAYLOAD_COLUMN)
            while (cursor.moveToNext()) {
                val word = canonicalWord(cursor.getString(wordIndex))
                result[word] = json.decodeFromString<WordEntry>(cursor.getString(payloadIndex)).copy(word = word)
            }
        }
        return result
    }

    fun progress(): Map<String, Progress> {
        val result = linkedMapOf<String, Progress>()
        readableDatabase.query(
            PROGRESS_TABLE,
            arrayOf(WORD_COLUMN, STRENGTH_COLUMN, RATED_AT_COLUMN, REVIEWS_COLUMN),
            null,
            null,
            null,
            null,
            "$WORD_COLUMN ASC",
        ).use { cursor ->
            val wordIndex = cursor.getColumnIndexOrThrow(WORD_COLUMN)
            val strengthIndex = cursor.getColumnIndexOrThrow(STRENGTH_COLUMN)
            val ratedAtIndex = cursor.getColumnIndexOrThrow(RATED_AT_COLUMN)
            val reviewsIndex = cursor.getColumnIndexOrThrow(REVIEWS_COLUMN)
            while (cursor.moveToNext()) {
                val word = canonicalWord(cursor.getString(wordIndex))
                result[word] = Progress(
                    word = word,
                    strength = cursor.getDouble(strengthIndex),
                    ratedAtMillis = cursor.getLong(ratedAtIndex),
                    reviews = cursor.getInt(reviewsIndex),
                )
            }
        }
        return result
    }

    fun excluded(): Set<String> {
        val result = linkedSetOf<String>()
        readableDatabase.query(
            EXCLUDED_TABLE,
            arrayOf(WORD_COLUMN),
            null,
            null,
            null,
            null,
            "$WORD_COLUMN ASC",
        ).use { cursor ->
            val wordIndex = cursor.getColumnIndexOrThrow(WORD_COLUMN)
            while (cursor.moveToNext()) {
                result += canonicalWord(cursor.getString(wordIndex))
            }
        }
        return result
    }

    fun saveEntry(entry: WordEntry) {
        val word = canonicalKey(entry.word)
        val values = ContentValues().apply {
            put(WORD_COLUMN, word)
            put(PAYLOAD_COLUMN, json.encodeToString(entry.copy(word = word)))
        }
        val rowId = writableDatabase.insertWithOnConflict(
            ENTRIES_TABLE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        requireInserted(ENTRIES_TABLE, rowId)
    }

    fun saveProgress(progress: Progress) {
        val word = canonicalKey(progress.word)
        val values = ContentValues().apply {
            put(WORD_COLUMN, word)
            put(STRENGTH_COLUMN, progress.strength)
            put(RATED_AT_COLUMN, progress.ratedAtMillis)
            put(REVIEWS_COLUMN, progress.reviews)
        }
        val rowId = writableDatabase.insertWithOnConflict(
            PROGRESS_TABLE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        requireInserted(PROGRESS_TABLE, rowId)
    }

    fun setExcluded(word: String, excluded: Boolean) {
        val key = canonicalKey(word)
        if (excluded) {
            val values = ContentValues().apply { put(WORD_COLUMN, key) }
            val rowId = writableDatabase.insertWithOnConflict(
                EXCLUDED_TABLE,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE,
            )
            requireInserted(EXCLUDED_TABLE, rowId)
        } else {
            writableDatabase.delete(EXCLUDED_TABLE, "$WORD_COLUMN = ?", arrayOf(key))
        }
    }

    private fun createTables(database: SQLiteDatabase) {
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS $ENTRIES_TABLE (
                $WORD_COLUMN TEXT PRIMARY KEY NOT NULL,
                $PAYLOAD_COLUMN TEXT NOT NULL
            )""".trimIndent(),
        )
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS $PROGRESS_TABLE (
                $WORD_COLUMN TEXT PRIMARY KEY NOT NULL,
                $STRENGTH_COLUMN REAL NOT NULL,
                $RATED_AT_COLUMN INTEGER NOT NULL,
                $REVIEWS_COLUMN INTEGER NOT NULL DEFAULT 1
            )""".trimIndent(),
        )
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS $EXCLUDED_TABLE (
                $WORD_COLUMN TEXT PRIMARY KEY NOT NULL
            )""".trimIndent(),
        )
    }

    private fun canonicalKey(word: String): String = canonicalWord(word).also {
        require(it.isNotEmpty()) { "word must not be blank" }
    }

    private fun requireInserted(table: String, rowId: Long) {
        if (rowId == -1L) throw SQLiteException("Could not save the $table row.")
    }

    companion object {
        const val DATABASE_NAME = "wordtiles.db"
        private const val DATABASE_VERSION = 1
        private const val ENTRIES_TABLE = "entries"
        private const val PROGRESS_TABLE = "progress"
        private const val EXCLUDED_TABLE = "excluded"
        private const val WORD_COLUMN = "word"
        private const val PAYLOAD_COLUMN = "payload"
        private const val STRENGTH_COLUMN = "strength"
        private const val RATED_AT_COLUMN = "rated_at"
        private const val REVIEWS_COLUMN = "reviews"
        private val json = Json { ignoreUnknownKeys = true }
    }
}
