package io.inai.android_sample_integration.drop_in

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import io.inai.android_sample_integration.BuildConfig
import io.inai.android_sample_integration.Config
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.helpers.NetworkRequestHandler
import io.inai.android_sample_integration.helpers.json
import io.inai.android_sample_integration.helpers.showAlert
import io.inai.android_sdk.*
import kotlinx.android.synthetic.main.fragment_present_checkout.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class PresentCheckoutFragment : Fragment(R.layout.fragment_present_checkout), InaiCheckoutDelegate {

    private val inaiBackendOrdersUrl: String = BuildConfig.BaseUrl + "/orders"
    private var orderId = ""
    private val orderMetadata: Map<String, JsonPrimitive> = mutableMapOf(
        "test_order_id" to JsonPrimitive("test_order")
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareOrder()
    }

    private fun prepareOrder() {
        showProgress()
        val orderPostData = getDataForOrders()
        val networkConfig = mutableMapOf(
            NetworkRequestHandler.KEY_URL to inaiBackendOrdersUrl,
            NetworkRequestHandler.KEY_REQUEST_TYPE to NetworkRequestHandler.POST,
            NetworkRequestHandler.KEY_POST_DATA_JSON to Json.encodeToString(orderPostData)
        )
        makeNetworkRequest(networkConfig, ::onOrderPrepared)
    }

    private fun onOrderPrepared(orderResponse: String) {
        hideProgress()
        val orderResult = json.decodeFromString<OrderResult>(orderResponse)
        Config.customerId = orderResult.customer_id
        orderId = orderResult.id
        initializeCheckout()
    }

    private fun initializeCheckout() {
        if (BuildConfig.InaiToken.isNotEmpty() && orderId.isNotEmpty()) {
            val config = InaiConfig(
                token = BuildConfig.InaiToken,
                orderId = orderId,
                countryCode = Config.countryCode,
                redirectUrl = ""
            )
            try {
                val inaiCheckout = InaiCheckout(config)
                inaiCheckout.presentCheckout(
                    requireContext(),
                    this
                )
            } catch (ex: Exception) {
                //  Handle initialisation error
                requireActivity().showAlert("Error while initialising sdk : $ex.message")
            }
        }
    }

    override fun paymentFinished(result: InaiPaymentResult) {
        when (result.status) {
            InaiPaymentStatus.Success -> {
                requireActivity().showAlert("Payment Success! ${result.data}")
            }
            InaiPaymentStatus.Failed -> {
                requireActivity().showAlert("Payment Failed! ${result.data}")
            }
            InaiPaymentStatus.Canceled -> {
                var message = "Payment Canceled!"
                if (result.data.has("message")) {
                    message = result.data.getString("message")
                }
                requireActivity().showAlert(message)
            }
        }
    }

    /**
     *  Helper Functions
     */
    private fun makeNetworkRequest(
        networkConfig: Map<String, String>,
        callback: (String) -> Unit
    ) {
        NetworkRequestHandler.makeNetworkRequest(networkConfig) { result: NetworkRequestHandler.Result ->
            when (result) {
                is NetworkRequestHandler.Result.Success -> {
                    callback(result.message)
                }
                is NetworkRequestHandler.Result.Failure -> {
                    onError(result.message)
                }
            }
        }
    }

    private fun getDataForOrders(): OrderPostData {
        return OrderPostData(
            amount = Config.amount,
            currency = Config.currency,
            customer = OrderCustomer(
                email = "customer@example.com",
                first_name = "John",
                last_name = "Doe",
                contact_number = "01010101010",
                id = Config.customerId
            ),
            metadata = JsonObject(orderMetadata)
        )
    }

    private fun onError(error: String) {
        hideProgress()
        requireActivity().showAlert(error)
    }

    private fun showProgress() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        progressBar.visibility = View.INVISIBLE
    }

    /**
     *  Fragment cycle callback.
     *  Here we cancel coroutine scope which in turn cancels any ongoing network operations
     */
    override fun onStop() {
        super.onStop()
        NetworkRequestHandler.cancelCoroutineScope()
        hideProgress()
    }
}