package io.inai.android_sample_integration.helpers

import android.content.Context
import io.inai.android_sample_integration.Config.countryCode
import io.inai.android_sample_integration.Config.inaiToken
import io.inai.android_sample_integration.helpers.Orders.orderId
import io.inai.android_sdk.*
import org.json.JSONObject


/**
 *  Helper class thich implements the InaiValidateFieldsDelegate and provides function for validating fields.
 */
class ValidateFieldsHelper(private val context: Context) : InaiValidateFieldsDelegate {

    //  Callback lambda which is invoked to notify the parent class about validation success.
    private var validateFieldsCallback: (String) -> Unit = {}

    /**
     *  Function which calls the sdk validateFieldsMethos.
     *  @param paymentMethodOption - Preferred payment option selected by the user.
     *  @param paymentDetails - The JSON Object which contains the user card details input.
     *  @param  validateFieldsResultCallback - This contains the post validation logic that is defined by the parent.
     */
    fun validateFields(
        paymentMethodOption: String,
        paymentDetails: JSONObject,
        validateFieldsResultCallback: (String) -> Unit
    ) {
        validateFieldsCallback = validateFieldsResultCallback
        //  Init Inai SDK
        val config = InaiConfig(
            token = inaiToken,
            orderId = orderId,
            countryCode = countryCode
        )
        try {
            val inaiCheckout = InaiCheckout(config)
            inaiCheckout.validateFields(
                paymentMethodOption,
                paymentDetails,
                context = context,
                delegate = this
            )
        } catch (ex: Exception) {
            //  Handle initialisation error
            context.showAlert("Error while initialising sdk : $ex.message")
        }
    }

    //   If validation returns success call makeHeadlessPayment()
    override fun fieldsValidationFinished(result: InaiValidateFieldsResult) {
        when (result.status) {
            InaiValidateFieldsStatus.Success -> {
                //  Invoke callback with success message
                validateFieldsCallback("Validate Fields Success Result : ${result.data}")
            }
            InaiValidateFieldsStatus.Failed -> {
                context.showAlert("Validate Fields Fail Result : ${result.data}")
            }
        }
    }
}