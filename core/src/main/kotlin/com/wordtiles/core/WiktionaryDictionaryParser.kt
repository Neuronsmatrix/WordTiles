package com.wordtiles.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val FREE_DICTIONARY_API_URL = "https://freedictionaryapi.com/"
private val wiktionaryJson = Json { ignoreUnknownKeys = true }

fun parseWiktionaryDictionary(json: String): WordEntry {
    val response = try {
        wiktionaryJson.parseToJsonElement(json) as? JsonObject
            ?: throw DictionaryParseException("Dictionary response is not an entry object.")
    } catch (error: DictionaryParseException) {
        throw error
    } catch (error: Exception) {
        throw DictionaryParseException("Dictionary response is malformed.", error)
    }

    val word = response.string("word")
    if (word.isBlank()) throw DictionaryParseException("Dictionary response is missing a word.")
    val entries = response.array("entries")
    if (entries.isEmpty()) throw DictionaryParseException("Dictionary response contains no entries.")

    val entryObjects = entries.mapIndexed { index, element ->
        element as? JsonObject
            ?: throw DictionaryParseException("Dictionary entry ${index + 1} is malformed.")
    }
    val meanings = entryObjects.mapIndexed { entryIndex, entry ->
        val definitions = entry.array("senses").flatMapIndexed { senseIndex, element ->
            val sense = element as? JsonObject
                ?: throw DictionaryParseException(
                    "Sense ${senseIndex + 1} in entry ${entryIndex + 1} is malformed.",
                )
            flattenSense(sense, emptyList())
        }
        Meaning(
            partOfSpeech = entry.string("partOfSpeech"),
            definitions = definitions,
            synonyms = entry.stringArray("synonyms"),
            antonyms = entry.stringArray("antonyms"),
        )
    }
    if (meanings.sumOf { it.definitions.size } == 0) {
        throw DictionaryParseException("Dictionary response contains no definitions.")
    }

    val phonetic = entryObjects.firstNotNullOfOrNull { entry ->
        entry.array("pronunciations").firstNotNullOfOrNull { element ->
            val pronunciation = element as? JsonObject ?: return@firstNotNullOfOrNull null
            pronunciation.string("text").takeIf {
                it.isNotBlank() && pronunciation.string("type").equals("ipa", ignoreCase = true)
            }
        }
    }.orEmpty()

    val source = response["source"] as? JsonObject
    val license = source?.get("license") as? JsonObject
    val sourceUrl = source?.string("url").orEmpty()
    val licenseName = license?.string("name").orEmpty()
    val licenseUrl = license?.string("url").orEmpty()
    val sources = buildList {
        if (sourceUrl.isNotBlank() || licenseName.isNotBlank() || licenseUrl.isNotBlank()) {
            add(Source(sourceUrl, licenseName, licenseUrl))
        }
        if (sourceUrl != FREE_DICTIONARY_API_URL) {
            add(Source(FREE_DICTIONARY_API_URL))
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

private fun flattenSense(sense: JsonObject, parentDefinitions: List<String>): List<Definition> {
    val rawDefinition = sense.string("definition")
    val tags = sense.stringArray("tags")
    val renderedDefinition = when {
        rawDefinition.isBlank() -> ""
        tags.isEmpty() -> rawDefinition
        else -> "[${tags.joinToString(", ")}] $rawDefinition"
    }
    val contextualDefinition = when {
        renderedDefinition.isBlank() -> ""
        parentDefinitions.isEmpty() -> renderedDefinition
        else -> "${parentDefinitions.joinToString(" › ")} — $renderedDefinition"
    }
    val examples = sense.stringArray("examples") + sense.array("quotes").mapNotNull { element ->
        val quote = element as? JsonObject
            ?: throw DictionaryParseException("Dictionary quote is malformed.")
        val text = quote.string("text")
        val reference = quote.string("reference")
        when {
            text.isBlank() -> null
            reference.isBlank() -> text
            else -> "$text — $reference"
        }
    }
    val ownDefinition = if (contextualDefinition.isBlank()) {
        emptyList()
    } else {
        listOf(
            Definition(
                text = contextualDefinition,
                example = examples.joinToString("\n"),
                synonyms = sense.stringArray("synonyms"),
                antonyms = sense.stringArray("antonyms"),
            ),
        )
    }
    val childContext = if (rawDefinition.isBlank()) {
        parentDefinitions
    } else {
        parentDefinitions + rawDefinition
    }
    val children = sense.array("subsenses").flatMap { element ->
        val child = element as? JsonObject
            ?: throw DictionaryParseException("Dictionary subsense is malformed.")
        flattenSense(child, childContext)
    }
    return ownDefinition + children
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
