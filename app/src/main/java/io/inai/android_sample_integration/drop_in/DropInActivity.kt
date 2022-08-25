package io.inai.android_sample_integration.drop_in

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import io.inai.android_sample_integration.BuildConfig
import io.inai.android_sample_integration.Config
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.helpers.NetworkRequestHandler
import io.inai.android_sample_integration.helpers.json
import io.inai.android_sample_integration.helpers.showAlert
import io.inai.android_sdk.*
import kotlinx.android.synthetic.main.activity_drop_in.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class DropInActivity : AppCompatActivity(), InaiCheckoutDelegate {

    private val inaiBackendOrdersUrl: String = BuildConfig.InaiBaseUrl + "/orders"
    private val authenticationString = NetworkRequestHandler.getEncodedAuthString(Config.inaiToken, Config.inaiPassword)
    private var orderId = ""
    private val orderMetadata: Map<String, JsonPrimitive> = mutableMapOf(
        "test_order_id" to JsonPrimitive("test_order"),
        "vat" to JsonPrimitive("6"),
        "tax_percentage" to JsonPrimitive("12"),
        "taxable_amount" to JsonPrimitive("50")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drop_in)
        //  Remove Title bar
        supportActionBar?.hide()
        prepareOrder()
    }

    private fun prepareOrder() {
        showProgress()
        val orderPostData = getDataForOrders()
        val networkConfig = mutableMapOf(
            NetworkRequestHandler.KEY_URL to inaiBackendOrdersUrl,
            NetworkRequestHandler.KEY_REQUEST_TYPE to NetworkRequestHandler.POST,
            NetworkRequestHandler.KEY_AUTH_STRING to authenticationString,
            NetworkRequestHandler.KEY_POST_DATA_JSON to Json.encodeToString(orderPostData)
        )
        makeNetworkRequest(networkConfig, ::onOrderPrepared)
    }

    private fun onOrderPrepared(orderResponse: String) {
        hideProgress()
        val orderResult = json.decodeFromString<OrderResult>(orderResponse)
        Config.customerId = orderResult.customer_id
        orderId = orderResult.id
        Log.d("ORDER_ID", orderId)
        initializeCheckout()
    }

    private fun initializeCheckout() {
        if (Config.inaiToken.isNotEmpty() && orderId.isNotEmpty()) {
            val config = InaiConfig(
                token = Config.inaiToken,
                orderId = orderId,
                countryCode = Config.countryCode,
                redirectUrl = ""
            )
            try {
                val inaiCheckout = InaiCheckout(config)
                inaiCheckout.presentCheckout(
                    this,
                    this
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
                email = "testdev@inai.io",
                first_name = "Dev",
                last_name = "Smith",
                contact_number = "01010101010",
                id = Config.customerId
            ),
            metadata = JsonObject(orderMetadata)
        )
    }

    private fun onError(error: String) {
        hideProgress()
        showAlert(error)
    }

    fun showProgress() {
        progressBar.visibility = View.VISIBLE
    }

    fun hideProgress() {
        progressBar.visibility = View.INVISIBLE
    }
}