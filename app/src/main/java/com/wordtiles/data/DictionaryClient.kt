package com.wordtiles.data

import com.wordtiles.core.DictionaryParseException
import com.wordtiles.core.WordEntry
import com.wordtiles.core.parseWiktionaryDictionary
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

sealed class DictionaryException(message: String, cause: Throwable? = null) : Exception(message, cause)

class DictionaryMissingEntryException(word: String) :
    DictionaryException("No dictionary entry was found for “$word”.")

class DictionaryNetworkException(message: String, cause: Throwable? = null) :
    DictionaryException(message, cause)

class DictionaryResponseException(message: String, cause: Throwable? = null) :
    DictionaryException(message, cause)

internal fun interface DictionaryProvider {
    suspend fun lookup(word: String): WordEntry
}

internal class FreeDictionaryProvider : DictionaryProvider {
    override suspend fun lookup(word: String): WordEntry = withContext(Dispatchers.IO) {
        val encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8.name()).replace("+", "%20")
        val connection = java.net.URL("$BASE_URL$encodedWord").openConnection() as HttpsURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = READ_TIMEOUT_MILLIS
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "WordTiles/0.1")

        try {
            when (val status = connection.responseCode) {
                HttpURLConnection.HTTP_NOT_FOUND -> throw DictionaryMissingEntryException(word)
                !in 200..299 -> throw DictionaryResponseException(
                    "The dictionary service returned HTTP $status. Please try again.",
                )
            }
            val contentLength = connection.contentLengthLong
            if (contentLength > MAX_RESPONSE_BYTES) {
                throw DictionaryResponseException("The dictionary response was too large to use safely.")
            }
            val payload = connection.inputStream.use(::readBoundedUtf8)
            coroutineContext.ensureActive()
            try {
                parseWiktionaryDictionary(payload)
            } catch (error: DictionaryParseException) {
                throw DictionaryResponseException("The dictionary returned an invalid entry.", error)
            }
        } catch (error: DictionaryException) {
            throw error
        } catch (error: IOException) {
            throw DictionaryNetworkException(
                "Could not reach the dictionary service. Check your connection and try again.",
                error,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun readBoundedUtf8(input: java.io.InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > MAX_RESPONSE_BYTES) {
                throw DictionaryResponseException("The dictionary response was too large to use safely.")
            }
            output.write(buffer, 0, count)
        }
        return output.toString(StandardCharsets.UTF_8.name())
    }

    companion object {
        private const val BASE_URL = "https://freedictionaryapi.com/api/v1/entries/en/"
        private const val CONNECT_TIMEOUT_MILLIS = 10_000
        private const val READ_TIMEOUT_MILLIS = 30_000
        private const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024
    }
}
