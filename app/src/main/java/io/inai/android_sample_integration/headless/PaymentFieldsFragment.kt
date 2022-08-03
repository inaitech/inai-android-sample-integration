package io.inai.android_sample_integration.headless

import android.os.Bundle
import io.inai.android_sample_integration.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import io.inai.android_sample_integration.Config.countryCode
import io.inai.android_sample_integration.Config.inaiToken
import io.inai.android_sample_integration.helpers.*
import io.inai.android_sample_integration.helpers.Orders.orderId
import io.inai.android_sample_integration.model.PaymentMethodOption
import io.inai.android_sdk.*
import kotlinx.android.synthetic.main.fragment_payment_fields.*
import org.json.JSONArray
import org.json.JSONObject


class PaymentFieldsFragment : Fragment(), InaiCheckoutDelegate {

    companion object {
        const val FIELD_TYPE_CHECKBOX = "checkbox"
        const val FIELD_TYPE_SELECT = "select"
    }

    private lateinit var paymentMethodOption: PaymentMethodOption
    private lateinit var formLayout: LinearLayout
    private lateinit var formBuilder: FormBuilder
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
        formBuilder = FormBuilder(requireContext())
        createFormFields()

        btn_proceed.setOnClickListener {
            if (validateFormInput()) {
                generatePaymentDetails()
                makeHeadlessPayment()
            }
        }
    }

    private fun createFormFields() {
        paymentMethodOption.formFields.forEachIndexed { _, formField ->
            formLayout.addView(formBuilder.createLabel(formField))
            when (formField.fieldType) {
                FIELD_TYPE_CHECKBOX -> {
                    formLayout.addView(formBuilder.createCheckBox(formField))
                }
                FIELD_TYPE_SELECT -> {
                    formLayout.addView(formBuilder.createPicker(formField))
                }
                else -> {
                    val editText = formBuilder.createTextField(formField)
                    // Add card expiry textWatchers if fields are for card expiry formatting
                    if (formField.name == "expiry") {
                        editText.addTextChangedListener(ExpiryDateFormatter(editText))
                    }
                    formLayout.addView(editText)
                }
            }
        }
    }

    private fun validateFormInput(): Boolean {
        var areFormInputsValid = true
        var areRequiredInputsFilled = true

        paymentMethodOption.formFields.forEach {
           if (it.fieldType != FIELD_TYPE_CHECKBOX && it.fieldType != FIELD_TYPE_SELECT ){
               val formFieldEditText = formLayout.findViewWithTag<FormFieldEditText>(it.name)
               when {
                   formFieldEditText.isInvalidInput() -> {
                       areFormInputsValid = false
                       return@forEach
                   }
                   formFieldEditText.isFieldEmpty() -> {
                       areRequiredInputsFilled = false
                       return@forEach
                   }
               }
           }
        }
        return areFormInputsValid && areRequiredInputsFilled
    }

    private fun generatePaymentDetails() {
        val fieldsArray = JSONArray()
        var paymentField: JSONObject
        // Get payment field JSON object based on field type.
        paymentMethodOption.formFields.forEach {

            paymentField = when (it.fieldType) {
                FIELD_TYPE_CHECKBOX -> {
                    val checkbox = formLayout.findViewWithTag<CheckBox>(it.name)
                    getPaymentField(
                        it.name,
                        checkbox?.isChecked ?: false
                    )
                }
                FIELD_TYPE_SELECT -> {
                    val spinner = formLayout.findViewWithTag<Spinner>(it.name)
                    val selection = it.data?.values?.single { item ->
                        item.label == spinner.selectedItem
                    }
                    getPaymentField(
                        it.name,
                        selection?.value ?: ""
                    )
                }
                else -> {
                    val formFieldEditText = formLayout.findViewWithTag<FormFieldEditText>(it.name)
                    getPaymentField(
                        it.name,
                        formFieldEditText?.text.toString()
                    )
                }
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
                showAlert("Payment Failed! ${result.data}")
            }
            InaiPaymentStatus.Canceled -> {
                var message = "Payment Canceled!"
                if (result.data.has("message")) {
                    message = result.data.getString("message")
                }
                showAlert(message)
            }
        }
    }
}