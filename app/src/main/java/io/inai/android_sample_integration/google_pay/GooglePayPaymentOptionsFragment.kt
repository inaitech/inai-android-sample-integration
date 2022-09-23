package io.inai.android_sample_integration.google_pay

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.inai.android_sample_integration.BuildConfig
import io.inai.android_sample_integration.Config
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.headless.HeadlessActivity
import io.inai.android_sample_integration.helpers.*
import kotlinx.android.synthetic.main.fragment_google_pay_payment_options.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class GooglePayPaymentOptionsFragment : Fragment(R.layout.fragment_google_pay_payment_options) {

    private var orderId = ""
    private val paymentOptionsAdapter: PaymentOptionsAdapter by lazy { PaymentOptionsAdapter() }
    private val authenticationString = NetworkRequestHandler.getEncodedAuthString(Config.inaiToken)
    private val inaiBackendOrdersUrl: String = BuildConfig.BaseUrl + "/orders"
    private val inaiBackendPaymentOptionsUrl: String = BuildConfig.BaseUrl + "/payment-method-options"
    private val orderMetadata: Map<String, JsonPrimitive> = mutableMapOf(
        "test_order_id" to JsonPrimitive("test_order")
    )

    companion object {
        const val GOOGLE_PAY = "google_pay"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareUi()
        prepareOrder()
    }

    private fun prepareUi() {
        paymentOptionsAdapter.clickListener = { paymentMethodOption ->
            // Start Google Pay activity
            val intent = Intent(requireActivity(), GooglePayActivity::class.java)
            val jsonString = Json.encodeToString(paymentMethodOption)
            intent.putExtra(GooglePayActivity.ARG_GPAY_PAYMENT_FIELDS, jsonString)
            intent.putExtra("order_id", orderId)
            startActivity(intent)
        }

        rv_payment_options.apply {
            adapter = paymentOptionsAdapter
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    private fun prepareOrder() {
        (activity as HeadlessActivity).showProgress()
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
        val orderResult = json.decodeFromString<OrderResult>(orderResponse)
        Config.customerId = orderResult.customer_id
        orderId = orderResult.id

        fetchPaymentOptions()
    }

    private fun fetchPaymentOptions() {
        val url = "$inaiBackendPaymentOptionsUrl?order_id=$orderId&country=${Config.countryCode}"
        val networkConfig = mutableMapOf(
            NetworkRequestHandler.KEY_URL to url,
            NetworkRequestHandler.KEY_AUTH_STRING to authenticationString,
            NetworkRequestHandler.KEY_REQUEST_TYPE to NetworkRequestHandler.GET
        )
        makeNetworkRequest(networkConfig, ::onPaymentOptionsFetched)
    }

    private fun onPaymentOptionsFetched(response: String) {
        (activity as HeadlessActivity).hideProgress()
        val paymentOptionsResult = json.decodeFromString<PaymentOptionsResult>(response)
        val paymentOptionsList = paymentOptionsResult.paymentMethodOptions
        if (paymentOptionsList.isNullOrEmpty()) {
            //  Show message payment options are not available
            onError("Payment options are not available")
        } else {
            // We only need to show google pay option here.
            val filteredList = paymentOptionsList.filter {
                it.railCode == GOOGLE_PAY
            }
            paymentOptionsAdapter.addList(filteredList)
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
        (activity as HeadlessActivity).hideProgress()
        this.showAlert(error)
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