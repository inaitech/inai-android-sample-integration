package io.inai.android_sample_integration.headless.validate_fields

import android.content.Context
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import io.inai.android_sample_integration.R
import java.util.regex.Pattern

class FormFieldEditText(context: Context, formField: FormField) : AppCompatEditText(context) {

    private var formField: FormField
    private var isInvalidInput = false
    private val errorInputColor = ResourcesCompat.getColor(resources, R.color.red_400, null)
    private val validInputColor = ResourcesCompat.getColor(resources, R.color.teal_200, null)
    private val textWatcher = object : TextWatcher{
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!text.isNullOrEmpty()) {
                verifyInputLength(text.toString())
                verifyInputWithRegex(text.toString())
            }
        }

        override fun afterTextChanged(s: Editable?) {

        }
    }

    init {
        this.formField = formField
        initTextChangedListener()
    }

    private fun initTextChangedListener() {
       addTextChangedListener(textWatcher)
    }

    private fun verifyInputLength(input: String) {
        val maxLength = formField.validations.max_length ?: 0
        val minLength = formField.validations.min_length ?: 0
        if (maxLength != 0 && minLength != 0) {
            if (input.length in minLength..maxLength) {
                showValidInputState()
            } else {
                showErrorState()
            }
        }
    }

    private fun verifyInputWithRegex(input: String) {
        val inputRegex = Pattern.compile(formField.validations.input_mask_regex ?: ".*")
        if (!inputRegex.matcher(input).matches())
            showErrorState()
        else
            showValidInputState()
    }

    fun isInvalidInput(): Boolean {
        return isInvalidInput
    }

    fun isFieldEmpty(): Boolean {
        return this.text.isNullOrEmpty()
    }

    private fun showErrorState() {
        ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(errorInputColor))
        isInvalidInput = true
    }

    private fun showValidInputState() {
        ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(validInputColor))
        isInvalidInput = false
    }
}