package io.inai.android_sample_integration.headless.save_payment_method

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.LinearLayout
import android.widget.Spinner
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.headless.HeadlessActivity
import io.inai.android_sample_integration.headless.make_payment.PaymentFieldsFragment
import io.inai.android_sample_integration.headless.make_payment.PaymentOptionsFragment
import io.inai.android_sample_integration.helpers.*
import io.inai.android_sample_integration.model.PaymentMethodOption
import kotlinx.android.synthetic.main.fragment_save_payment_method.*
import org.json.JSONArray
import org.json.JSONObject

class SavePaymentMethod : Fragment(R.layout.fragment_save_payment_method) {

    private lateinit var paymentMethodOption: PaymentMethodOption
    private lateinit var formLayout: LinearLayout
    private lateinit var formBuilder: FormBuilder
    private lateinit var makePaymentHelper: MakePaymentHelper
    private val paymentDetails = JSONObject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        paymentMethodOption =
            arguments?.getParcelable<PaymentMethodOption>(PaymentOptionsFragment.ARG_PAYMENT_OPTION) as PaymentMethodOption
        formLayout = view.findViewById(R.id.form_layout)
        formBuilder = FormBuilder(requireContext())
        makePaymentHelper = MakePaymentHelper(requireContext())
        createFormFields()

        btn_save_payment_method.setOnClickListener {
            if (validateFormInput()) {
                generatePaymentDetails()
                makePayment()
            }
        }
    }

    private fun createFormFields() {
        paymentMethodOption.formFields.forEachIndexed { _, formField ->
            //  Since, we do not need Save Card checkbox for Save Payment Method operation,
            //  we ignore the save card field and do not show the checkbox.
            if (formField.name == "save_card") return@forEachIndexed

            formLayout.addView(formBuilder.createLabel(formField))
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

        //  Since we are saving a payment method, the value will always
        //  be true for field type save_card. So we do not check for the
        //  field type explicitly. Instead we dirctly add it to the JSON Array.
        fieldsArray.put(
            getPaymentField("save_card", true)
        )

        paymentDetails.put("fields", fieldsArray)
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

    /**
     *  Fragment cycle callback.
     *  Here we cancel coroutine scope which in turn cancels any ongoing network operations
     */
    override fun onStop() {
        super.onStop()
        NetworkRequestHandler.cancelCoroutineScope()
        (activity as HeadlessActivity).hideProgress()
    }

}