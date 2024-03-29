package io.inai.android_sample_integration.headless.make_payment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import io.inai.android_sample_integration.R

class FormBuilder(private val context: Context) {

    companion object {
        const val FIELD_TYPE_CHECKBOX = "checkbox"
        const val FIELD_TYPE_SELECT = "select"
        const val FIELD_TYPE_RADIO = "radio"
    }

    fun createLabel(formField: FormField): TextView {
        //  Create form field label
        val label = TextView(context)
        label.text = formField.label + if (formField.required!!) "*" else ""
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        label.setTextColor(ResourcesCompat.getColor(context.resources, R.color.black, null))
        label.tag = "label"

        return label
    }

    fun createTextField(formField: FormField): FormFieldEditText {

        //  Create form field inputText field
        val editText = FormFieldEditText(context, formField)
        editText.inputType = InputType.TYPE_CLASS_TEXT
        editText.hint = formField.placeholder
        editText.tag = formField.name

        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.bottomMargin = 16
        editText.layoutParams = layoutParams

        return editText
    }

    fun createCheckBox(formField: FormField): CheckBox {
        val checkBox = CheckBox(context)
        checkBox.text = formField.label
        checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        checkBox.tag = formField.name
        return checkBox
    }

    @SuppressLint("NewApi")
    fun createButton(mode: Mode):Button{
        val button = Button(context)
        button.text = "Pay"
        button.tag = mode
        button.isEnabled = true
        button.setBackgroundColor(context.getColor(R.color.teal_700))
        return button
    }

    fun createRadioButtonGroup(formField: FormField): RadioGroup {
        val rg = RadioGroup(context)
        rg.tag = formField
        formField.data?.values?.forEach {
            val radioButton = RadioButton(context)
            radioButton.text = it.label
            radioButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            radioButton.tag = it.value
            rg.addView(radioButton)
        }
        return rg
    }

    fun createRadioButtonGroupByRailCode(paymentMethods: List<PaymentMethodOption>): RadioGroup {
        val rg = RadioGroup(context)
        rg.tag = paymentMethods
        paymentMethods.forEach{
            val radioButton = RadioButton(context)
            radioButton.text = it.railCode
            radioButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            radioButton.tag = it.railCode
            rg.addView(radioButton)
        }
        return rg
    }
    fun createPicker(formField: FormField): Spinner {
        val label = TextView(context)
        label.text = formField.label + if (formField.required!!) "*" else ""
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        label.setTextColor(ResourcesCompat.getColor(context.resources, R.color.black, null))
        label.tag = "label"

        val picker = Spinner(context)
        val countryList: List<String> = formField.data!!.values!!.map {
            it.label!!
        }
        // Create an ArrayAdapter using the string array and a default spinner layout
        val adapter: ArrayAdapter<String> = ArrayAdapter(
            context, android.R.layout.simple_spinner_dropdown_item,
            countryList.toMutableList()
        )
        picker.tag = formField.name
        picker.adapter = adapter
        picker.prompt = formField.label

        return picker
    }

}