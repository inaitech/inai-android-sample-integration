package io.inai.android_sample_integration

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
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

/**
 * Alert Dialog
 */
fun Fragment.showAlert(message: String) {
    val alertBuilder: AlertDialog.Builder = AlertDialog.Builder(this.requireContext())
    alertBuilder.setMessage(message)
    alertBuilder.setCancelable(true)

    alertBuilder.setPositiveButton(
        "Ok"
    ) { dialog, _ ->
        findNavController().navigateUp()
    }

    val alertDialog: AlertDialog = alertBuilder.create()
    alertDialog.show()
}
