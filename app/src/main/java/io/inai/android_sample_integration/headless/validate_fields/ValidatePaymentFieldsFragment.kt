package io.inai.android_sample_integration.headless.validate_fields

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import io.inai.android_sample_integration.Config
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.headless.save_payment_method.SavePaymentMethod_PaymentOptionsFragment
import io.inai.android_sample_integration.headless.validate_fields.FormBuilder.Companion.FIELD_TYPE_CHECKBOX
import io.inai.android_sample_integration.headless.validate_fields.FormBuilder.Companion.FIELD_TYPE_SELECT
import io.inai.android_sample_integration.helpers.*
import io.inai.android_sdk.*
import kotlinx.android.synthetic.main.fragment_validate_payment_fields.*
import org.json.JSONArray
import org.json.JSONObject

class ValidatePaymentFieldsFragment : Fragment(R.layout.fragment_validate_payment_fields), InaiValidateFieldsDelegate {

    private lateinit var paymentMethodOption: PaymentMethodOption
    private lateinit var formLayout: LinearLayout
    private lateinit var formBuilder: FormBuilder
    private lateinit var orderId: String
    private lateinit var validateHelper: ValidateFieldsHelper
    private val paymentDetails = JSONObject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        paymentMethodOption =
            arguments?.getParcelable<PaymentMethodOption>(ValidateFields_PaymentOptionsFragment.ARG_PAYMENT_OPTION) as PaymentMethodOption
        orderId = arguments?.getString(SavePaymentMethod_PaymentOptionsFragment.ARG_ORDER_ID) ?: ""
        formLayout = view.findViewById(R.id.form_layout)
        formBuilder = FormBuilder(requireContext())
        validateHelper = ValidateFieldsHelper(requireContext())
        createFormFields()
        btn_validate_fields.setOnClickListener {
            generatePaymentDetails()
            validateFields()
        }
    }

    private fun createFormFields() {
        paymentMethodOption.formFields.forEachIndexed { _, formField ->
            formLayout.addView(formBuilder.createLabel(formField))
            when (formField.fieldType) {
                FormBuilder.FIELD_TYPE_CHECKBOX -> {
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
    }

    //  Returns a JSON Object with name, value key pairs for payment details.
    private fun getPaymentField(name: String, value: Any): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("name", name)
        jsonObject.put("value", value)
        return jsonObject
    }

    private fun validateFields() {
        //  Init Inai SDK
        val config = InaiConfig(
            token = Config.inaiToken,
            orderId = Orders.orderId,
            countryCode = Config.countryCode
        )
        try {
            val inaiCheckout = InaiCheckout(config)
            inaiCheckout.validateFields(
                paymentMethodOption.railCode,
                paymentDetails,
                context = requireContext(),
                delegate = this
            )
        } catch (ex: Exception) {
            //  Handle initialisation error
            requireContext().showAlert("Error while initialising sdk : $ex.message")
        }
    }

    override fun fieldsValidationFinished(result: InaiValidateFieldsResult) {
        when (result.status) {
            InaiValidateFieldsStatus.Success -> {
                //  Invoke callback with success message
                requireContext().showAlert("Validate Fields Success Result : ${result.data}")
            }
            InaiValidateFieldsStatus.Failed -> {
                requireContext().showAlert("Validate Fields Fail Result : ${result.data}")
            }
        }
    }
}