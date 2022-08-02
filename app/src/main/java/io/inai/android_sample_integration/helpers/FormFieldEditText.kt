package io.inai.android_sample_integration.helpers

import android.content.Context
import android.content.res.ColorStateList
import android.text.Editable
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.doAfterTextChanged
import io.inai.android_sample_integration.model.FormField
import io.inai.android_sdk.R
import java.util.regex.Pattern

class FormFieldEditText(context: Context, formField: FormField) : AppCompatEditText(context) {

    private lateinit var formField: FormField
    private var isInputValid = false
    private val errorInputColor =ResourcesCompat.getColor(resources, R.color.teal_200, null)
    private val validInputColor =ResourcesCompat.getColor(resources, R.color.teal_200, null)

    init{
        this.formField = formField
        initTextChangedListener()
    }

    private fun initTextChangedListener(){
        doAfterTextChanged { editText: Editable? ->
            verifyInputLength(editText.toString())
            verifyInputWithRegex(editText.toString())
        }
    }

    private fun verifyInputLength(input: String){
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

    private fun verifyInputWithRegex(input: String){
        val inputRegex = Pattern.compile(formField.validations.input_mask_regex ?: ".*")
        if (!inputRegex.matcher(input).matches())
            showErrorState()
        else
            showValidInputState()
    }

    fun isInputValid(): Boolean{
        return isInputValid
    }

    fun isFieldEmpty(): Boolean{
        return this.text.isNullOrEmpty()
    }

    private fun showErrorState(){
        ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(errorInputColor))
        isInputValid = false
    }

    private fun showValidInputState(){
        ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(validInputColor))
        isInputValid = true
    }
}