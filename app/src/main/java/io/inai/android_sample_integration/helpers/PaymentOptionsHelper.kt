package io.inai.android_sample_integration.helpers

import io.inai.android_sample_integration.BuildConfig
import io.inai.android_sample_integration.model.PaymentMethod
import io.inai.android_sample_integration.model.PaymentMethodOption
import io.inai.android_sample_integration.model.PaymentMethodsResult
import io.inai.android_sample_integration.model.PaymentOptionsResult
import kotlinx.serialization.decodeFromString

/**
 *  A helper class which handles the fetching of payment options list and saved payment methods list.
 */
class PaymentOptionsHelper {

    private val inaiBackendPaymentOptionsUrl: String = BuildConfig.InaiBaseUrl + "payment-method-options"
    private val inaiBackendSavedPaymentMethod: String = BuildConfig.InaiBaseUrl + "customers/"

    /**
     *  This lambda function is invoked whwnever we get an error state.
     *  The error handling logic should be defined by the parent class
     */
    var errorCallback: (String) -> Unit = {}

    /**
     *  This function fetches the available payment options.
     *  In case od savePaymentMethods this fetches available payment options for the
     *  saved methods.
     *  @param paramsMap is the list of query params passed by the parent class to append
     *  to the url.
     *  @param paymentOptionsResultCallback is invoked when we get the API response and the
     *  result is sent back to the parent.
     */
    fun fetchPaymentOptions(
        paramsMap: Map<String, String>,
        paymentOptionsResultCallback: (List<PaymentMethodOption>) -> Unit
    ) {
        val url = getPaymentOptionsUrl(paramsMap)
        NetworkRequestHandler.makeGetRequest(
            url,
            Orders.authenticationString
        ) { result: NetworkRequestHandler.Result ->
            when (result) {
                is NetworkRequestHandler.Result.Success -> {
                    val paymentOptionsResult = json.decodeFromString<PaymentOptionsResult>(result.message)
                    val list = paymentOptionsResult.paymentMethodOptions
                    if (list.isNullOrEmpty()) {
                        //  Show message payment options are not available
                        errorCallback("Payment options are not available")
                    } else {
                        //  Pass payment options list to caller
                        paymentOptionsResultCallback(list)
                    }
                }
                is NetworkRequestHandler.Result.Failure -> errorCallback(result.message)
            }
        }

    }

    /**
     *  This function fetches the savedPaymentMethods.
     *  @param customerId should be passed by the parent.
     *  @param savedMethodsResultCallback is invoked when we get the API response and the
     *  result is sent back to the parent.
     */
    fun fetchSavedPaymentMethods(
        customerId: String,
        savedMethodsResultCallback: (List<PaymentMethod>) -> Unit
    ) {
        val url = getSavedPaymentMethodsUrl(customerId)
        NetworkRequestHandler.makeGetRequest(
            url,
            Orders.authenticationString
        ) { result ->
            when (result) {
                is NetworkRequestHandler.Result.Success -> {
                    val paymentsMethodsResult = json.decodeFromString<PaymentMethodsResult>(result.message)
                    val list = paymentsMethodsResult.paymentMethods
                    if (list.isNullOrEmpty()) {
                        errorCallback("Saved Payment Methods are not available")
                    } else {
                        //  Pass payment methods list to caller.
                        savedMethodsResultCallback(list)
                    }
                }
                is NetworkRequestHandler.Result.Failure -> errorCallback(result.message)
            }
        }

    }

    /**
     *  Helper function which adds query params to the payment options url by looping through
     *  the map.
     *  @param paramsMap contains the list of query params.
     */
    private fun getPaymentOptionsUrl(paramsMap: Map<String, String>): String {
        var paramString = ""
        paramsMap.keys.forEachIndexed { keyIndex, key ->
            paramString = paramString.plus("${key}=${paramsMap[key]}")
            if (keyIndex != paramsMap.size - 1) paramString = paramString.plus("&")
        }
        return "$inaiBackendPaymentOptionsUrl?$paramString"
    }

    /**
     *  Helper function which adds the customer ID as a path parameter to the
     *  savedPaymentMethods url.
     *  @param customerId should be passed by the parent.
     */
    private fun getSavedPaymentMethodsUrl(customerId: String): String {
        return "$inaiBackendSavedPaymentMethod${customerId}/payment-methods"
    }

}