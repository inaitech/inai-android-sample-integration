package io.inai.android_sample_integration.headless.validate_fields

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.inai.android_sample_integration.BuildConfig
import io.inai.android_sample_integration.Config
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.headless.HeadlessActivity
import io.inai.android_sample_integration.helpers.*
import kotlinx.android.synthetic.main.fragment_validate_fields_payment_options.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class ValidateFields_PaymentOptionsFragment : Fragment(R.layout.fragment_validate_fields_payment_options) {

    private val inaiBackendOrdersUrl: String = BuildConfig.InaiBaseUrl + "/orders"
    private val inaiBackendPaymentOptionsUrl: String = BuildConfig.InaiBaseUrl + "/payment-method-options"
    private val authenticationString = NetworkRequestHandler.getEncodedAuthString(Config.inaiToken, Config.inaiPassword)
    private var orderId = ""
    private val orderMetadata: Map<String, JsonPrimitive> = mutableMapOf(
        "test_order_id" to JsonPrimitive("test_order"),
        "vat" to JsonPrimitive("6"),
        "tax_percentage" to JsonPrimitive("12"),
        "taxable_amount" to JsonPrimitive("50")
    )
    private val paymentOptionsAdapter: PaymentOptionsAdapter by lazy { PaymentOptionsAdapter() }
    private val bundle = Bundle()

    companion object {
        const val ARG_ORDER_ID = "order_id"
        const val ARG_PAYMENT_OPTION = "arg_payment_option"
        const val APPLE_PAY = "apple_pay"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareUi()
        prepareOrder()
    }

    private fun prepareUi() {
        paymentOptionsAdapter.clickListener = { paymentMethodOption ->
            //  Navigate to validate screen to proceed with the selected payment option
            bundle.apply {
                putParcelable(ARG_PAYMENT_OPTION, paymentMethodOption as Parcelable)
                putString(ARG_ORDER_ID, orderId)
            }
            goToPaymentScreen()
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
            // We do not need apple_pay to be shown on android app since apple pay will not work on android.
            val filteredList = paymentOptionsList.filter {
                it.railCode != APPLE_PAY
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
        (activity as HeadlessActivity).hideProgress()
        this.showAlert(error)
    }

    private fun goToPaymentScreen() {
        findNavController().navigate(
            R.id.action_validateFieldsPaymentOptionsFragment_to_validatePaymentFieldsFragment,
            bundle
        )
    }

    override fun onStop() {
        super.onStop()
        (activity as HeadlessActivity).hideProgress()
        NetworkRequestHandler.cancelCoroutineScope()
    }
}