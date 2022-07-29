package io.inai.android_sample_integration

import kotlinx.serialization.json.Json

/***
 *  String Utild
 */
fun getSanitizedText(text: String): String {
    return if (text.isNotEmpty()) {
        text
            .split('_')
            .map { it.trim() }
            .map { capitalizeFirst(it) }
            .fold("") { string, element ->
                string.plus(element.plus(" "))
            }
    } else ""
}

fun capitalizeFirst(word: String): String {
    return (word.substring(0, 1).uppercase(java.util.Locale.getDefault())
            + word.substring(1).lowercase(java.util.Locale.getDefault()))
}

/***
 *  Json Utils
 */

val json = Json { ignoreUnknownKeys = true }
