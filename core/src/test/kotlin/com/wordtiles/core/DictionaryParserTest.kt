package com.wordtiles.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DictionaryParserTest {
    @Test
    fun `preserves every homograph meaning definition and relation in response order`() {
        val entry = parseDictionary(MULTI_HOMOGRAPH_RESPONSE)

        assertEquals("lead", entry.word)
        assertEquals("/liːd/", entry.phonetic)
        assertEquals(MULTI_HOMOGRAPH_RESPONSE, entry.rawResponse)
        assertEquals(3, entry.meanings.size)
        assertEquals("verb", entry.meanings[0].partOfSpeech)
        assertEquals(
            listOf("guide", "be ahead of"),
            entry.meanings[0].synonyms,
        )
        assertEquals(listOf("follow"), entry.meanings[0].antonyms)
        assertEquals(2, entry.meanings[0].definitions.size)
        assertEquals(
            Definition(
                text = "To guide or conduct.",
                example = "She will lead the discussion.",
                synonyms = listOf("direct"),
                antonyms = listOf("trail"),
            ),
            entry.meanings[0].definitions[0],
        )
        assertEquals(
            Definition(text = "To be in front."),
            entry.meanings[0].definitions[1],
        )
        assertEquals("noun", entry.meanings[1].partOfSpeech)
        assertEquals("A position of advantage.", entry.meanings[1].definitions.single().text)
        assertEquals("noun", entry.meanings[2].partOfSpeech)
        assertEquals("A dense metallic element.", entry.meanings[2].definitions.single().text)
    }

    @Test
    fun `keeps source URLs with the license that governs each homograph`() {
        val entry = parseDictionary(MULTI_HOMOGRAPH_RESPONSE)

        assertEquals(
            listOf(
                Source(
                    url = "https://en.wiktionary.org/wiki/lead",
                    licenseName = "CC BY-SA 3.0",
                    licenseUrl = "https://creativecommons.org/licenses/by-sa/3.0",
                ),
                Source(
                    url = "https://en.wiktionary.org/wiki/lead#English",
                    licenseName = "CC BY-SA 4.0",
                    licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0",
                ),
            ),
            entry.sources,
        )
    }

    @Test
    fun `uses empty values for optional text arrays and attribution fields`() {
        val entry = parseDictionary(
            """[{"word":"laconic","meanings":[{"partOfSpeech":"adjective","definitions":[{"definition":"Using few words."}]}]}]""",
        )

        assertEquals("", entry.phonetic)
        assertEquals(emptyList<String>(), entry.meanings.single().synonyms)
        assertEquals(emptyList<String>(), entry.meanings.single().antonyms)
        assertEquals("", entry.meanings.single().definitions.single().example)
        assertEquals(emptyList<String>(), entry.meanings.single().definitions.single().synonyms)
        assertEquals(emptyList<String>(), entry.meanings.single().definitions.single().antonyms)
        assertEquals(emptyList<Source>(), entry.sources)
    }

    @Test
    fun `retains response license metadata when no source URL is supplied`() {
        val entry = parseDictionary(
            """
            [{
              "word":"liminal",
              "meanings":[{"partOfSpeech":"adjective","definitions":[{"definition":"At a threshold."}]}],
              "license":{"name":"CC BY-SA 3.0","url":"https://creativecommons.org/licenses/by-sa/3.0"}
            }]
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                Source(
                    url = "",
                    licenseName = "CC BY-SA 3.0",
                    licenseUrl = "https://creativecommons.org/licenses/by-sa/3.0",
                ),
            ),
            entry.sources,
        )
    }

    @Test
    fun `rejects an API error object`() {
        assertThrows(DictionaryParseException::class.java) {
            parseDictionary("""{"title":"No Definitions Found","message":"Sorry pal."}""")
        }
    }

    @Test
    fun `rejects malformed JSON empty results and entries without definitions`() {
        listOf(
            "not json",
            "[]",
            """[{"word":"empty","meanings":[]}]""",
            """[{"word":"empty","meanings":[{"partOfSpeech":"noun","definitions":[]}]}]""",
        ).forEach { response ->
            assertThrows(DictionaryParseException::class.java) {
                parseDictionary(response)
            }
        }
    }

    companion object {
        private val MULTI_HOMOGRAPH_RESPONSE =
            """
            [
              {
                "word": "lead",
                "phonetic": "/liːd/",
                "meanings": [
                  {
                    "partOfSpeech": "verb",
                    "definitions": [
                      {
                        "definition": "To guide or conduct.",
                        "example": "She will lead the discussion.",
                        "synonyms": ["direct"],
                        "antonyms": ["trail"]
                      },
                      {"definition": "To be in front."}
                    ],
                    "synonyms": ["guide", "be ahead of"],
                    "antonyms": ["follow"]
                  },
                  {
                    "partOfSpeech": "noun",
                    "definitions": [{"definition": "A position of advantage."}]
                  }
                ],
                "license": {
                  "name": "CC BY-SA 3.0",
                  "url": "https://creativecommons.org/licenses/by-sa/3.0"
                },
                "sourceUrls": ["https://en.wiktionary.org/wiki/lead"]
              },
              {
                "word": "lead",
                "phonetic": "/lɛd/",
                "meanings": [
                  {
                    "partOfSpeech": "noun",
                    "definitions": [{"definition": "A dense metallic element."}]
                  }
                ],
                "license": {
                  "name": "CC BY-SA 4.0",
                  "url": "https://creativecommons.org/licenses/by-sa/4.0"
                },
                "sourceUrls": ["https://en.wiktionary.org/wiki/lead#English"]
              }
            ]
            """.trimIndent()
    }
}
