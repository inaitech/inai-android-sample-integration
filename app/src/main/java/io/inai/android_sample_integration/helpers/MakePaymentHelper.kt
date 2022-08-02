package io.inai.android_sample_integration.helpers

import android.content.Context
import io.inai.android_sample_integration.Config
import io.inai.android_sdk.*
import org.json.JSONObject

/**
 *  A helper class which handles the MakePayment method of SDK.
 */
class MakePaymentHelper(private val context: Context) : InaiCheckoutDelegate {

    //  Callback lambda which is invoked to notify the parent class about headless payment status.
    var makePaymentCallback: (String) -> Unit = {}

    fun makeHeadlessPayment(
        paymentMethodOption: String,
        paymentDetails: JSONObject,
        makePaymentResultCallback: (String) -> Unit
    ) {
        makePaymentCallback = makePaymentResultCallback
        if (Config.inaiToken.isNotEmpty()) {
            val config = InaiConfig(
                token = Config.inaiToken,
                orderId = Orders.orderId,
                countryCode = Config.countryCode,
                redirectUrl = ""
            )
            try {
                val inaiCheckout = InaiCheckout(config)
                inaiCheckout.makePayment(
                    paymentMethodOption,
                    paymentDetails,
                    context = context,
                    delegate = this
                )
            } catch (ex: Exception) {
                //  Handle initialisation error
                makePaymentCallback("Error while initialising sdk : $ex.message")
            }
        }
    }

    override fun paymentFinished(result: InaiPaymentResult) {
        when (result.status) {
            InaiPaymentStatus.Success -> {
                makePaymentCallback("Payment Success! ${result.data}")
            }
            InaiPaymentStatus.Failed -> {
                makePaymentCallback("Payment Failed! ${result.data}")
            }
            InaiPaymentStatus.Canceled -> {
                var message = "Payment Canceled!"
                if (result.data.has("message")) {
                    message = result.data.getString("message")
                }
                makePaymentCallback(message)
            }
        }
    }
}