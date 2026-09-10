package com.wordtiles.core

import java.time.LocalDate
import kotlin.random.Random

fun selectDailyTopicIds(
    day: LocalDate,
    topicIds: List<String>,
    count: Int = 10,
): List<String> {
    require(count >= 0) { "count must not be negative" }
    val distinctIds = topicIds.distinct().sorted()
    require(distinctIds.size >= count) { "catalog must contain at least $count distinct topics" }
    if (count == 0) return emptyList()

    return distinctIds
        .shuffled(Random(day.toEpochDay().hashCode()))
        .take(count)
}
