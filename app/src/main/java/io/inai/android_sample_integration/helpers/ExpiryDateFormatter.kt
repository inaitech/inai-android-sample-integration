package io.inai.android_sample_integration.helpers

import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.widget.EditText

/**
 *  This class implements TextWatcher interface and formats card expiry date into the format MM/YY
 *  @param editText The input field to observe input and format it.
 */
class ExpiryDateFormatter(private val editText: EditText) : TextWatcher {
    //   Variable to keep track of the formatted string.
    private var formattedExpiryDate = ""

    override fun beforeTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {

    }

    override fun onTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
        val inputText = charSequence?.toString().orEmpty()
        // Check necessary so that we format fresh inputs and not the same string.
        //  because once we set the edit text with formatted string onTextChanged will be
        //  triggered again for the formatted string.
        if (inputText != formattedExpiryDate) {

            formattedExpiryDate = when {
                //  Append a slash if length is 2 and slash is not yet added.
                inputText.length == 2 && !formattedExpiryDate.endsWith("/") ->
                    "$inputText/"

                //  This case handles a delete operation.
                //  For Ex. InputText = 12 and formattedExpiryDate = 12/ then a delete
                //  operation deletes the 2 along with the slash.
                inputText.length == 2 && formattedExpiryDate.endsWith("/") ->
                    //  Valid month check
                    if (inputText.toInt() <= 12) inputText.substring(0, 1) else ""

                //  If input is the first character and its above 1 (for ex: 5)
                //  then we assume its the 5th month and format it as 05
                inputText.length == 1 ->
                    if (inputText.toInt() > 1) "0$inputText/" else inputText

                else ->
                    inputText
            }

            //  Set the editText with the formatted string.
            editText.setText(formattedExpiryDate)
            //  Set the cursor position to the end of the string.
            editText.setSelection(formattedExpiryDate.length)
        }
    }

    override fun afterTextChanged(charSequence: Editable?) {

    }

    init {
        //  Add a filter to the edit text constraining maximum character entry to 5
        // Two characters for Month, One for Slash and Two for Year.
        editText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(5))
        //  Specify input type as numbers.
        editText.inputType = InputType.TYPE_CLASS_NUMBER
    }
}