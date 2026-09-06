package com.wordtiles.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class DictionaryParseException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

private val dictionaryJson = Json { ignoreUnknownKeys = true }

fun parseDictionary(json: String): WordEntry {
    val response = try {
        dictionaryJson.parseToJsonElement(json) as? JsonArray
            ?: throw DictionaryParseException("Dictionary response is not an entry list.")
    } catch (error: DictionaryParseException) {
        throw error
    } catch (error: Exception) {
        throw DictionaryParseException("Dictionary response is malformed.", error)
    }
    if (response.isEmpty()) {
        throw DictionaryParseException("Dictionary response contains no entries.")
    }

    val entries = response.mapIndexed { index, element ->
        element as? JsonObject
            ?: throw DictionaryParseException("Dictionary entry ${index + 1} is malformed.")
    }
    val word = entries.firstNotNullOfOrNull { it.string("word").takeIf(String::isNotBlank) }
        ?: throw DictionaryParseException("Dictionary response is missing a word.")
    val phonetic = entries.firstNotNullOfOrNull { entry ->
        entry.string("phonetic").takeIf(String::isNotBlank)
            ?: entry.array("phonetics").firstNotNullOfOrNull { item ->
                (item as? JsonObject)?.string("text")?.takeIf(String::isNotBlank)
            }
    }.orEmpty()

    var definitionCount = 0
    val meanings = entries.flatMapIndexed { entryIndex, entry ->
        entry.array("meanings").mapIndexed { meaningIndex, element ->
            val meaning = element as? JsonObject
                ?: throw DictionaryParseException(
                    "Meaning ${meaningIndex + 1} in entry ${entryIndex + 1} is malformed.",
                )
            val definitions = meaning.array("definitions").mapIndexed { definitionIndex, item ->
                val definition = item as? JsonObject
                    ?: throw DictionaryParseException(
                        "Definition ${definitionIndex + 1} in entry ${entryIndex + 1} is malformed.",
                    )
                val text = definition.string("definition")
                if (text.isBlank()) {
                    throw DictionaryParseException("Dictionary response contains a definition without text.")
                }
                definitionCount += 1
                Definition(
                    text = text,
                    example = definition.string("example"),
                    synonyms = definition.stringArray("synonyms"),
                    antonyms = definition.stringArray("antonyms"),
                )
            }
            Meaning(
                partOfSpeech = meaning.string("partOfSpeech"),
                definitions = definitions,
                synonyms = meaning.stringArray("synonyms"),
                antonyms = meaning.stringArray("antonyms"),
            )
        }
    }
    if (definitionCount == 0) {
        throw DictionaryParseException("Dictionary response contains no definitions.")
    }

    val sources = entries.flatMap { entry ->
        val license = entry["license"] as? JsonObject
        val licenseName = license?.string("name").orEmpty()
        val licenseUrl = license?.string("url").orEmpty()
        val sourceUrls = entry.stringArray("sourceUrls")
        when {
            sourceUrls.isNotEmpty() -> sourceUrls.map { sourceUrl ->
                Source(sourceUrl, licenseName, licenseUrl)
            }
            licenseName.isNotBlank() || licenseUrl.isNotBlank() -> listOf(
                Source(url = "", licenseName = licenseName, licenseUrl = licenseUrl),
            )
            else -> emptyList()
        }
    }

    return WordEntry(
        word = word,
        phonetic = phonetic,
        meanings = meanings,
        sources = sources,
        rawResponse = json,
    )
}

private fun JsonObject.string(name: String): String =
    (get(name) as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()

private fun JsonObject.array(name: String): JsonArray =
    get(name)?.let { element ->
        element as? JsonArray
            ?: throw DictionaryParseException("Dictionary field '$name' must be a list.")
    } ?: JsonArray(emptyList())

private fun JsonObject.stringArray(name: String): List<String> = array(name).map { element ->
    (element as? JsonPrimitive)?.takeIf { it.isString }?.content
        ?: throw DictionaryParseException("Dictionary field '$name' must contain text values.")
}
