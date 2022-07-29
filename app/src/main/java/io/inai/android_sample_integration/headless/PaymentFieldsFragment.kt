package io.inai.android_sample_integration.headless

import android.os.Bundle
import io.inai.android_sample_integration.R
import android.text.*
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import io.inai.android_sample_integration.Config.countryCode
import io.inai.android_sample_integration.Config.inaiToken
import io.inai.android_sample_integration.Orders.orderId
import io.inai.android_sample_integration.model.FormField
import io.inai.android_sample_integration.model.PaymentMethodOption
import io.inai.android_sample_integration.showAlert
import io.inai.android_sdk.*
import kotlinx.android.synthetic.main.fragment_payment_fields.*
import org.json.JSONArray
import org.json.JSONObject


class PaymentFieldsFragment : Fragment(), InaiCheckoutDelegate{

    companion object {
        const val FIELD_TYPE_CHECKBOX = "checkbox"
    }

    private lateinit var paymentMethodOption: PaymentMethodOption
    private lateinit var formLayout: LinearLayout
    private val paymentDetails = JSONObject()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payment_fields, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        paymentMethodOption =
            arguments?.getSerializable(PaymentOptionsFragment.ARG_PAYMENT_OPTION) as PaymentMethodOption
        formLayout = view.findViewById(R.id.form_layout)

        createFormFields()

        btn_proceed.setOnClickListener {
            generatePaymentDetails()
            makeHeadlessPayment()
        }
    }

    private fun createFormFields() {
        paymentMethodOption.formFields.forEachIndexed { _, formField ->
            when (formField.fieldType) {
                FIELD_TYPE_CHECKBOX -> createCheckBox(formField)
                else -> createTextField(formField)
            }
        }
    }

    private fun createTextField(formField: FormField) {
        //  Create form field label
        val label = TextView(requireContext())
        label.text = formField.label + if (formField.required) "*" else ""
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        label.setTextColor(ResourcesCompat.getColor(resources, R.color.black, null))
        label.tag = "label"

        //  Create form field inputText field
        val inputTextField = EditText(requireContext())
        inputTextField.inputType = InputType.TYPE_CLASS_TEXT
        inputTextField.hint = formField.placeholder
        inputTextField.tag = formField.name

        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.bottomMargin = 16
        inputTextField.layoutParams = layoutParams

        formLayout.addView(label)
        formLayout.addView(inputTextField)

    }

    private fun createCheckBox(formField: FormField) {
        val checkBox = CheckBox(requireContext())
        checkBox.text = formField.label
        checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        checkBox.tag = formField.name
        formLayout.addView(checkBox)
    }

    private fun generatePaymentDetails() {
        val fieldsArray = JSONArray()
        var paymentField: JSONObject
        // Get payment field JSON object based on field type.
        paymentMethodOption.formFields.forEach {

            paymentField = if (it.fieldType == FIELD_TYPE_CHECKBOX) {
                val inputTextFieldCheckbox = formLayout.findViewWithTag<CheckBox>(it.name)
                getPaymentField(
                    it.name,
                    inputTextFieldCheckbox?.isChecked ?: false
                )
            } else {
                val inputTextFieldTextBox = formLayout.findViewWithTag<EditText>(it.name)
                getPaymentField(
                    it.name,
                    inputTextFieldTextBox?.text.toString()
                )
            }

            fieldsArray.put(paymentField)
        }

        paymentDetails.put("fields", fieldsArray)
    }

    //  Returns a JSON Object with name, value key pairs for payment details.
    private fun getPaymentField(name: String, value: Any): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("name", name)
        jsonObject.put("value", value)
        return jsonObject
    }

    //  Call SDK method makePayment() to initiate a headless checkout.
    private fun makeHeadlessPayment() {
        if (inaiToken.isNotEmpty()) {
            val config = InaiConfig(
                token = inaiToken,
                orderId = orderId,
                countryCode = countryCode,
                redirectUrl = ""
            )
            Log.d("CONFIG","$config")
            try {
                val inaiCheckout = InaiCheckout(config)
                inaiCheckout.makePayment(
                    paymentMethodOption.railCode,
                    paymentDetails,
                    context = requireContext(),
                    delegate = this
                )
            } catch (ex: Exception) {
                //  Handle initialisation error
                showAlert("Error while initialising sdk : $ex.message")
            }
        }
    }

    override fun paymentFinished(result: InaiPaymentResult) {
        when (result.status) {
            InaiPaymentStatus.Success -> {
                showAlert("Payment Success! ${result.data}")
            }
            InaiPaymentStatus.Failed -> {
                showAlert("Payment Success! ${result.data}")
            }
            InaiPaymentStatus.Canceled -> {
                var message = "Payment Canceled!"
                if (result.data.has("message")) {
                    message = result.data.getString("message")
                }
                showAlert("Payment Success! ${result.data}")
            }
        }
    }
}