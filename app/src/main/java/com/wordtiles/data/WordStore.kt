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

data class CollectionMark(
    val saved: Boolean = false,
    val starred: Boolean = false,
) {
    val included: Boolean get() = saved || starred
}

data class DailyTopics(
    val day: String,
    val topicIds: List<String>,
)

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
        if (oldVersion < 2) {
            database.execSQL(
                """INSERT OR IGNORE INTO $COLLECTION_TABLE ($WORD_COLUMN, $SAVED_COLUMN, $STARRED_COLUMN)
                    SELECT $WORD_COLUMN, 1, 0 FROM $PROGRESS_TABLE""".trimIndent(),
            )
        }
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

    fun collection(): Map<String, CollectionMark> {
        val result = linkedMapOf<String, CollectionMark>()
        readableDatabase.query(
            COLLECTION_TABLE,
            arrayOf(WORD_COLUMN, SAVED_COLUMN, STARRED_COLUMN),
            null,
            null,
            null,
            null,
            "$WORD_COLUMN ASC",
        ).use { cursor ->
            val wordIndex = cursor.getColumnIndexOrThrow(WORD_COLUMN)
            val savedIndex = cursor.getColumnIndexOrThrow(SAVED_COLUMN)
            val starredIndex = cursor.getColumnIndexOrThrow(STARRED_COLUMN)
            while (cursor.moveToNext()) {
                val mark = CollectionMark(
                    saved = cursor.getInt(savedIndex) != 0,
                    starred = cursor.getInt(starredIndex) != 0,
                )
                if (mark.included) result[canonicalWord(cursor.getString(wordIndex))] = mark
            }
        }
        return result
    }

    fun dailyTopics(): DailyTopics? {
        readableDatabase.query(
            DAILY_TOPICS_TABLE,
            arrayOf(DAY_COLUMN, TOPIC_IDS_COLUMN),
            "$SLOT_COLUMN = ?",
            arrayOf(DAILY_TOPICS_SLOT.toString()),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return DailyTopics(
                day = cursor.getString(cursor.getColumnIndexOrThrow(DAY_COLUMN)),
                topicIds = json.decodeFromString(cursor.getString(cursor.getColumnIndexOrThrow(TOPIC_IDS_COLUMN))),
            )
        }
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
        saveProgress(writableDatabase, progress)
    }

    fun setSaved(word: String, saved: Boolean) {
        setCollectionFlag(word, SAVED_COLUMN, saved)
    }

    fun setStarred(word: String, starred: Boolean) {
        setCollectionFlag(word, STARRED_COLUMN, starred)
    }

    fun saveDailyTopics(value: DailyTopics) {
        require(value.day.isNotBlank()) { "day must not be blank" }
        require(value.topicIds.none(String::isBlank)) { "topic IDs must not be blank" }
        require(value.topicIds.size == value.topicIds.distinct().size) { "topic IDs must be distinct" }
        val values = ContentValues().apply {
            put(SLOT_COLUMN, DAILY_TOPICS_SLOT)
            put(DAY_COLUMN, value.day)
            put(TOPIC_IDS_COLUMN, json.encodeToString(value.topicIds))
        }
        val rowId = writableDatabase.insertWithOnConflict(
            DAILY_TOPICS_TABLE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        requireInserted(DAILY_TOPICS_TABLE, rowId)
    }

    internal fun saveProgressAndSetSaved(progress: Progress) {
        val word = canonicalKey(progress.word)
        val database = writableDatabase
        database.beginTransaction()
        try {
            requireCached(database, word)
            saveProgress(database, progress.copy(word = word))
            setCollectionFlag(database, word, SAVED_COLUMN, enabled = true)
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    private fun saveProgress(database: SQLiteDatabase, progress: Progress) {
        val word = canonicalKey(progress.word)
        val values = ContentValues().apply {
            put(WORD_COLUMN, word)
            put(STRENGTH_COLUMN, progress.strength)
            put(RATED_AT_COLUMN, progress.ratedAtMillis)
            put(REVIEWS_COLUMN, progress.reviews)
        }
        val rowId = database.insertWithOnConflict(
            PROGRESS_TABLE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        requireInserted(PROGRESS_TABLE, rowId)
    }

    private fun setCollectionFlag(word: String, column: String, enabled: Boolean) {
        val key = canonicalKey(word)
        val database = writableDatabase
        database.beginTransaction()
        try {
            if (enabled) requireCached(database, key)
            setCollectionFlag(database, key, column, enabled)
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    private fun setCollectionFlag(
        database: SQLiteDatabase,
        word: String,
        column: String,
        enabled: Boolean,
    ) {
        val current = collectionMark(database, word)
        val updated = when (column) {
            SAVED_COLUMN -> current.copy(saved = enabled)
            STARRED_COLUMN -> current.copy(starred = enabled)
            else -> error("Unknown collection flag: $column")
        }
        if (!updated.included) {
            database.delete(COLLECTION_TABLE, "$WORD_COLUMN = ?", arrayOf(word))
            return
        }
        val values = ContentValues().apply {
            put(WORD_COLUMN, word)
            put(SAVED_COLUMN, if (updated.saved) 1 else 0)
            put(STARRED_COLUMN, if (updated.starred) 1 else 0)
        }
        val rowId = database.insertWithOnConflict(
            COLLECTION_TABLE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        requireInserted(COLLECTION_TABLE, rowId)
    }

    private fun collectionMark(database: SQLiteDatabase, word: String): CollectionMark {
        database.query(
            COLLECTION_TABLE,
            arrayOf(SAVED_COLUMN, STARRED_COLUMN),
            "$WORD_COLUMN = ?",
            arrayOf(word),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (!cursor.moveToFirst()) return CollectionMark()
            return CollectionMark(
                saved = cursor.getInt(cursor.getColumnIndexOrThrow(SAVED_COLUMN)) != 0,
                starred = cursor.getInt(cursor.getColumnIndexOrThrow(STARRED_COLUMN)) != 0,
            )
        }
    }

    private fun requireCached(database: SQLiteDatabase, word: String) {
        database.query(
            ENTRIES_TABLE,
            arrayOf(WORD_COLUMN),
            "$WORD_COLUMN = ?",
            arrayOf(word),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            require(cursor.moveToFirst()) { "word must be cached before it can be added to collection" }
        }
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
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS $COLLECTION_TABLE (
                $WORD_COLUMN TEXT PRIMARY KEY NOT NULL,
                $SAVED_COLUMN INTEGER NOT NULL DEFAULT 0,
                $STARRED_COLUMN INTEGER NOT NULL DEFAULT 0
            )""".trimIndent(),
        )
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS $DAILY_TOPICS_TABLE (
                $SLOT_COLUMN INTEGER PRIMARY KEY NOT NULL,
                $DAY_COLUMN TEXT NOT NULL,
                $TOPIC_IDS_COLUMN TEXT NOT NULL
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
        private const val DATABASE_VERSION = 2
        private const val ENTRIES_TABLE = "entries"
        private const val PROGRESS_TABLE = "progress"
        private const val EXCLUDED_TABLE = "excluded"
        private const val COLLECTION_TABLE = "collection"
        private const val DAILY_TOPICS_TABLE = "daily_topics"
        private const val WORD_COLUMN = "word"
        private const val PAYLOAD_COLUMN = "payload"
        private const val STRENGTH_COLUMN = "strength"
        private const val RATED_AT_COLUMN = "rated_at"
        private const val REVIEWS_COLUMN = "reviews"
        private const val SAVED_COLUMN = "saved"
        private const val STARRED_COLUMN = "starred"
        private const val SLOT_COLUMN = "slot"
        private const val DAY_COLUMN = "day"
        private const val TOPIC_IDS_COLUMN = "topic_ids"
        private const val DAILY_TOPICS_SLOT = 1
        private val json = Json { ignoreUnknownKeys = true }
    }
}
