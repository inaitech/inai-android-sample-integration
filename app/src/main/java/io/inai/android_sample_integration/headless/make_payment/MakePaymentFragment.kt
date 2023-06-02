package io.inai.android_sample_integration.headless.make_payment

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.fragment.app.Fragment
import io.inai.android_sample_integration.BuildConfig
import io.inai.android_sample_integration.Config.countryCode
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.helpers.ExpiryDateFormatter
import io.inai.android_sample_integration.headless.make_payment.FormBuilder.Companion.FIELD_TYPE_CHECKBOX
import io.inai.android_sample_integration.headless.make_payment.FormBuilder.Companion.FIELD_TYPE_RADIO
import io.inai.android_sample_integration.headless.make_payment.FormBuilder.Companion.FIELD_TYPE_SELECT
import io.inai.android_sample_integration.helpers.Constants
import io.inai.android_sample_integration.helpers.showAlert
import io.inai.android_sdk.*
import kotlinx.android.synthetic.main.fragment_make_payment.*
import org.json.JSONArray
import org.json.JSONObject


class MakePaymentFragment : Fragment(R.layout.fragment_make_payment), InaiCheckoutDelegate {

    private lateinit var paymentMethodOption: PaymentMethodOption
    private lateinit var formLayout: LinearLayout
    private lateinit var formBuilder: FormBuilder
    private lateinit var orderId: String
    private val paymentDetails = JSONObject()
    lateinit var walletPaymentOptions:List<PaymentMethodOption>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        orderId = arguments?.getString(MakePayment_PaymentOptionsFragment.ARG_ORDER_ID) ?: ""
        paymentMethodOption =
            arguments?.getParcelable<PaymentMethodOption>(MakePayment_PaymentOptionsFragment.ARG_PAYMENT_OPTION) as PaymentMethodOption
        formLayout = view.findViewById(R.id.form_layout)
        formBuilder = FormBuilder(requireContext())
        walletPaymentOptions = MakePayment_PaymentOptionsFragment.PaymentMethodOptionsList.filter { it.category == "Wallet" }
        createFormFields()

        btn_proceed.setOnClickListener {
            if (validateFormInput()) {
                generatePaymentDetails()
                makeHeadlessPayment()
            }
        }
    }

    private fun createFormFields() {
        //for wallets there is no form fields so rendering the UI based on rail code
        if(paymentMethodOption.category == Constants.CATEGORY_WALLET){
            if (walletPaymentOptions != null && walletPaymentOptions.isNotEmpty())
                formLayout.addView(formBuilder.createRadioButtonGroupByRailCode(walletPaymentOptions))
        }
        if (paymentMethodOption.modes != null && paymentMethodOption.modes!!.isNotEmpty()) {
            paymentMethodOption.modes?.forEach {
                if (it.formFields.isNotEmpty() && it.supported_platforms.contains("MOBILE")) {
                    it.formFields.forEachIndexed { _, formField ->
                        formLayout.addView(formBuilder.createLabel(formField))
                        when (formField.fieldType) {
                            FIELD_TYPE_CHECKBOX -> {
                                formLayout.addView(formBuilder.createCheckBox(formField))
                            }
                            FIELD_TYPE_SELECT -> {
                                formLayout.addView(formBuilder.createPicker(formField))
                            }
                            FIELD_TYPE_RADIO -> {
                                formLayout.addView(formBuilder.createRadioButtonGroup(formField))
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

            }
        } else {
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
    }

    private fun validateFormInput(): Boolean {
        var areFormInputsValid = true
        var areRequiredInputsFilled = true

        if (paymentMethodOption.modes != null && paymentMethodOption.modes!!.isNotEmpty()) {
            paymentMethodOption.modes?.forEach {
                if (it.formFields.isNotEmpty() && it.supported_platforms.contains("MOBILE")) {
                    it.formFields.forEach {
                        if (it.fieldType != FIELD_TYPE_CHECKBOX && it.fieldType != FIELD_TYPE_SELECT) {
                            val formFieldEditText =
                                formLayout.findViewWithTag<FormFieldEditText>(it.name)
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
                }
            }
        }else{
            paymentMethodOption.formFields.forEach {
                if (it.fieldType != FIELD_TYPE_CHECKBOX && it.fieldType != FIELD_TYPE_SELECT) {
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
        }

        return areFormInputsValid && areRequiredInputsFilled
    }

    private fun generatePaymentDetails() {
        val fieldsArray = JSONArray()
        var paymentField: JSONObject
        var mode: String ?= null
        //for wallets there is no form fields so generating paymentdetails based rail code
        if(paymentMethodOption.category == Constants.CATEGORY_WALLET){
            val radioGroup = formLayout.findViewWithTag<RadioGroup>(walletPaymentOptions)
            val radioButton = formLayout.findViewById<RadioButton>(radioGroup.checkedRadioButtonId)
            if(radioButton!=null){
                var selectedRailCode = radioButton.tag.toString()
                paymentMethodOption.railCode = selectedRailCode
            }
            return
        }
        if (paymentMethodOption.modes != null && paymentMethodOption.modes!!.isNotEmpty()) {
            paymentMethodOption.modes?.forEach {
                if (it.formFields.isNotEmpty()) {
                    mode = it.code
                    it.formFields.forEach {
                        paymentField = when (it.fieldType) {
                            FIELD_TYPE_CHECKBOX -> {
                                val checkbox = formLayout.findViewWithTag<CheckBox>(it.name)
                                getPaymentField(
                                    it.name!!,
                                    checkbox?.isChecked ?: false
                                )
                            }
                            FIELD_TYPE_SELECT -> {
                                val spinner = formLayout.findViewWithTag<Spinner>(it.name)
                                val selection = it.data?.values?.single { item ->
                                    item.label == spinner.selectedItem
                                }
                                getPaymentField(
                                    it.name!!,
                                    selection?.value ?: ""
                                )
                            }
                            FIELD_TYPE_RADIO -> {
                                val radioGroup = formLayout.findViewWithTag<RadioGroup>(it)
                                val radioButton = formLayout.findViewById<RadioButton>(radioGroup.checkedRadioButtonId)

                                if(radioButton!=null){
                                    var selectedValue = radioButton.tag.toString()
                                    getPaymentField(it.name!!,selectedValue)
                                }
                                return
                            }
                            else -> {
                                val formFieldEditText =
                                    formLayout.findViewWithTag<FormFieldEditText>(it.name)
                                getPaymentField(
                                    it.name!!,
                                    formFieldEditText?.text.toString()
                                )
                            }
                        }
                        fieldsArray.put(paymentField)
                    }
                }
            }
            paymentDetails.put("mode", mode)
            paymentDetails.put("fields", fieldsArray)
        }else{
            paymentMethodOption.formFields.forEach {

                paymentField = when (it.fieldType) {
                    FIELD_TYPE_CHECKBOX -> {
                        val checkbox = formLayout.findViewWithTag<CheckBox>(it.name)
                        getPaymentField(
                            it.name!!,
                            checkbox?.isChecked ?: false
                        )
                    }
                    FIELD_TYPE_SELECT -> {
                        val spinner = formLayout.findViewWithTag<Spinner>(it.name)
                        val selection = it.data?.values?.single { item ->
                            item.label == spinner.selectedItem
                        }
                        getPaymentField(
                            it.name!!,
                            selection?.value ?: ""
                        )
                    }
                    else -> {
                        val formFieldEditText = formLayout.findViewWithTag<FormFieldEditText>(it.name)
                        getPaymentField(
                            it.name!!,
                            formFieldEditText?.text.toString()
                        )
                    }
                }

                fieldsArray.put(paymentField)
            }

            paymentDetails.put("fields", fieldsArray)
        }
        // Get payment field JSON object based on field type.

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
        if (BuildConfig.InaiToken.isNotEmpty() && orderId.isNotEmpty()) {
            val config = InaiConfig(
                token = BuildConfig.InaiToken,
                orderId = orderId,
                countryCode = countryCode,
                redirectUrl = ""
            )
            try {
                val inaiCheckout = InaiCheckout(config)
                inaiCheckout.makePayment(
                    paymentMethodOption.railCode!!,
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