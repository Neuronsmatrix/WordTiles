package com.wordtiles.core

import kotlinx.serialization.Serializable

@Serializable
data class WordEntry(
    val word: String,
    val phonetic: String = "",
    val meanings: List<Meaning>,
    val sources: List<Source> = emptyList(),
    val rawResponse: String = "",
)

@Serializable
data class Meaning(
    val partOfSpeech: String,
    val definitions: List<Definition>,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
)

@Serializable
data class Definition(
    val text: String,
    val example: String = "",
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
)

@Serializable
data class Source(
    val url: String,
    val licenseName: String = "",
    val licenseUrl: String = "",
)
