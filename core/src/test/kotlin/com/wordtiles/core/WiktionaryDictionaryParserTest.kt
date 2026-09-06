package com.wordtiles.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WiktionaryDictionaryParserTest {
    @Test
    fun `parses the live cogent response shape without losing definitions or attribution`() {
        val response = requireNotNull(javaClass.getResource("/cogent-response.json")).readText().trimEnd()
        val entry = parseWiktionaryDictionary(response)

        assertEquals("cogent", entry.word)
        assertEquals("/ˈkəʊd͡ʒn̩t/", entry.phonetic)
        assertEquals(1, entry.meanings.size)
        assertEquals("adjective", entry.meanings.single().partOfSpeech)
        assertEquals(3, entry.meanings.single().definitions.size)
        assertEquals(
            listOf("compelling", "conclusive", "convincing", "indisputable"),
            entry.meanings.single().synonyms,
        )
        assertEquals(
            "The prosecution presented a cogent argument, convincing the jury of the defendant's guilt.",
            entry.meanings.single().definitions[2].example,
        )
        assertEquals(
            listOf(
                Source(
                    "https://en.wiktionary.org/wiki/cogent",
                    "CC BY-SA 4.0",
                    "https://creativecommons.org/licenses/by-sa/4.0/",
                ),
                Source("https://freedictionaryapi.com/"),
            ),
            entry.sources,
        )
        assertEquals(response, entry.rawResponse)
    }

    @Test
    fun `flattens nested senses in order with context qualifiers examples quotes and relations`() {
        val entry = parseWiktionaryDictionary(NESTED_RESPONSE)

        assertEquals(2, entry.meanings.size)
        assertEquals("verb", entry.meanings[0].partOfSpeech)
        assertEquals(3, entry.meanings[0].definitions.size)
        assertEquals("[formal] To examine.", entry.meanings[0].definitions[0].text)
        assertEquals(
            "To examine. — To inspect an account closely.",
            entry.meanings[0].definitions[1].text,
        )
        assertEquals(
            "First example.\nSecond example.\nQuoted use. — Reference work, 2020.",
            entry.meanings[0].definitions[1].example,
        )
        assertEquals(listOf("audit"), entry.meanings[0].definitions[1].synonyms)
        assertEquals(listOf("skim"), entry.meanings[0].definitions[1].antonyms)
        assertEquals(
            "To examine. › To inspect an account closely. — [rare] To inspect repeatedly.",
            entry.meanings[0].definitions[2].text,
        )
        assertEquals("noun", entry.meanings[1].partOfSpeech)
        assertEquals("A close examination.", entry.meanings[1].definitions.single().text)
    }

    @Test
    fun `rejects malformed payloads and responses with no definitions`() {
        listOf(
            "not json",
            "{}",
            """{"word":"empty","entries":[]}""",
            """{"word":"empty","entries":[{"partOfSpeech":"noun","senses":[]}]}""",
            """{"word":"empty","entries":[{"partOfSpeech":"noun","senses":[{"tags":[]}]}]}""",
        ).forEach { response ->
            assertThrows(DictionaryParseException::class.java) {
                parseWiktionaryDictionary(response)
            }
        }
    }

    companion object {
        private val NESTED_RESPONSE =
            """
            {
              "word": "scrutiny",
              "entries": [
                {
                  "partOfSpeech": "verb",
                  "pronunciations": [{"type":"ipa","text":"/first/"}],
                  "senses": [{
                    "definition": "To examine.",
                    "tags": ["formal"],
                    "examples": [],
                    "quotes": [],
                    "synonyms": [],
                    "antonyms": [],
                    "subsenses": [{
                      "definition": "To inspect an account closely.",
                      "tags": [],
                      "examples": ["First example.", "Second example."],
                      "quotes": [{"text":"Quoted use.","reference":"Reference work, 2020."}],
                      "synonyms": ["audit"],
                      "antonyms": ["skim"],
                      "subsenses": [{
                        "definition":"To inspect repeatedly.",
                        "tags":["rare"],
                        "examples":[],"quotes":[],"synonyms":[],"antonyms":[],"subsenses":[]
                      }]
                    }]
                  }],
                  "synonyms": ["inspect"],
                  "antonyms": ["ignore"]
                },
                {
                  "partOfSpeech": "noun",
                  "pronunciations": [{"type":"ipa","text":"/second/"}],
                  "senses": [{
                    "definition":"A close examination.",
                    "tags":[],"examples":[],"quotes":[],"synonyms":[],"antonyms":[],"subsenses":[]
                  }],
                  "synonyms": [],
                  "antonyms": []
                }
              ],
              "source": {"url":"https://en.wiktionary.org/wiki/scrutiny","license":{"name":"CC BY-SA 4.0","url":"license"}}
            }
            """.trimIndent()
    }
}
