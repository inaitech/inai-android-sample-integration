package io.inai.android_sample_integration.headless

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Spinner
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.helpers.*
import io.inai.android_sample_integration.model.PaymentMethodOption
import kotlinx.android.synthetic.main.fragment_pay_with_saved_payment_method.*
import org.json.JSONArray
import org.json.JSONObject


class PayWithSavedPaymentMethod : Fragment() {

    private lateinit var paymentMethodOption: PaymentMethodOption
    private lateinit var formLayout: LinearLayout
    private lateinit var formBuilder: FormBuilder
    private lateinit var makePaymentHelper: MakePaymentHelper
    private lateinit var paymentMethodId: String
    private val paymentDetails = JSONObject()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pay_with_saved_payment_method, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        paymentMethodOption =
            arguments?.getSerializable(PaymentOptionsFragment.ARG_PAYMENT_OPTION) as PaymentMethodOption
        paymentMethodId =
            arguments?.getString(PaymentOptionsFragment.ARG_PAYMENT_METHOD_ID, "") ?: ""
        formLayout = view.findViewById(R.id.form_layout)
        formBuilder = FormBuilder(requireContext())
        makePaymentHelper = MakePaymentHelper(requireContext())
        createFormFields()

        btn_proceed.setOnClickListener {
            if (validateFormInput()) {
                generatePaymentDetails()
                makePayment()
            }
        }
    }

    private fun createFormFields() {
        paymentMethodOption.formFields.forEachIndexed { _, formField ->
            //  Only required information from user is captured for this method.
            //  since all other information will already be captured.
            //  So we show only text input fields.
            formLayout.addView(formBuilder.createLabel(formField))
            if (formField.fieldType != PaymentFieldsFragment.FIELD_TYPE_SELECT &&
                formField.fieldType != PaymentFieldsFragment.FIELD_TYPE_CHECKBOX
            )
                when (formField.fieldType) {
                    PaymentFieldsFragment.FIELD_TYPE_SELECT -> {
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
            if (it.fieldType != PaymentFieldsFragment.FIELD_TYPE_CHECKBOX && it.fieldType != PaymentFieldsFragment.FIELD_TYPE_SELECT) {
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
                PaymentFieldsFragment.FIELD_TYPE_SELECT -> {
                    val picker = formLayout.findViewWithTag<Spinner>(it.name)
                    val selection = it.data?.values?.single { item ->
                        item.label == picker.selectedItem
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
        //  Add the saved payment method ID.
        paymentDetails.put("paymentMethodId", paymentMethodId)
    }

    //  Returns a JSON Object with name, value key pairs for payment details.
    private fun getPaymentField(name: String, value: Any): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("name", name)
        jsonObject.put("value", value)
        return jsonObject
    }

    private fun makePayment() {
        val makePaymentResultCallback = { resultMsg: String ->
            showAlert(resultMsg)
        }

        makePaymentHelper.makeHeadlessPayment(
            paymentMethodOption.railCode, paymentDetails, makePaymentResultCallback
        )
    }
}