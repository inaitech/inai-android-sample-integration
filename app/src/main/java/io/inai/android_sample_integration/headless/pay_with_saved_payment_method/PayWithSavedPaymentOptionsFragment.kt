package io.inai.android_sample_integration.headless.pay_with_saved_payment_method

import android.content.Intent
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
import io.inai.android_sample_integration.Config.customerId
import io.inai.android_sample_integration.google_pay.GooglePayActivity
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.headless.HeadlessActivity
import io.inai.android_sample_integration.headless.make_payment.MakePayment_PaymentOptionsFragment
import io.inai.android_sample_integration.helpers.*
import kotlinx.android.synthetic.main.fragment_pay_with_saved_payment_options.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class PayWithSavedPaymentOptionsFragment : Fragment(R.layout.fragment_pay_with_saved_payment_options) {

    private var savedPaymentMethodId = ""
    private var savedPaymentMethodType = ""
    private var orderId = ""
    private val authenticationString = NetworkRequestHandler.getEncodedAuthString(Config.inaiToken, Config.inaiPassword)
    private val inaiBackendOrdersUrl: String = BuildConfig.BaseUrl + "/orders"
    private val inaiBackendPaymentOptionsUrl: String = BuildConfig.BaseUrl + "/payment-method-options"
    private val inaiBackendSavedPaymentMethod: String = BuildConfig.BaseUrl + "/customers/"
    private val savedPaymentMethodsAdapter: SavedPaymentsMethodAdapter by lazy { SavedPaymentsMethodAdapter() }
    private val bundle = Bundle()
    private val orderMetadata: Map<String, JsonPrimitive> = mutableMapOf(
        "test_order_id" to JsonPrimitive("test_order")
    )

    companion object {
        const val ARG_ORDER_ID = "order_id"
        const val ARG_PAYMENT_OPTION = "arg_payment_option"
        const val ARG_PAYMENT_METHOD_ID = "arg-payment_method_id"
        const val APPLE_PAY = "apple_pay"
        const val GOOGLE_PAY = "google_pay"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareUi()
        prepareOrder()
    }

    private fun prepareUi() {
        savedPaymentMethodsAdapter.clickListener = { paymentMethod ->
            //  Store the payment method id of the selected saved payment to pass as
            //  an argument to the payments screen
            savedPaymentMethodId = paymentMethod.id ?: ""
            //  Store the paymentMethodType of the selected saved payment method. This will be
            //  used later to filter out the payment fields for that type
            savedPaymentMethodType = paymentMethod.type ?: ""
            //  Fetch payment method options for the selected saved payment method
            //  by adding saved_payment_method=true param to query map.
            (activity as HeadlessActivity).showProgress()
            fetchPaymentOptions()
        }

        rv_saved_payment_methods.apply {
            adapter = savedPaymentMethodsAdapter
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
        customerId = orderResult.customer_id
        orderId = orderResult.id

        fetchSavedPaymentMethods()
    }

    private fun fetchSavedPaymentMethods(){
        val url = "$inaiBackendSavedPaymentMethod$customerId/payment-methods"
        val networkConfig = mutableMapOf(
            NetworkRequestHandler.KEY_URL to url,
            NetworkRequestHandler.KEY_AUTH_STRING to authenticationString,
            NetworkRequestHandler.KEY_REQUEST_TYPE to NetworkRequestHandler.GET
        )
        makeNetworkRequest(networkConfig, ::onSavedPaymentMethodsFetched)
    }

    private fun onSavedPaymentMethodsFetched(response: String){
        (activity as HeadlessActivity).hideProgress()
        val paymentsMethodsResult = json.decodeFromString<PaymentMethodsResult>(response)
        val savedPaymentMethodList = paymentsMethodsResult.paymentMethods
        if (savedPaymentMethodList.isNullOrEmpty()){
            onError("Saved Payment Methods are not available")
        }else{
            val filteredList = savedPaymentMethodList.filter {
                it.type != APPLE_PAY
            }
            savedPaymentMethodsAdapter.addList(filteredList)
        }
    }

    private fun fetchPaymentOptions() {
        val url = "$inaiBackendPaymentOptionsUrl?order_id=$orderId&country=${Config.countryCode}&saved_payment_method=true"
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
            (activity as HeadlessActivity).hideProgress()
            //  Filters the paymentMethodOptions to get the payment fields for the selected
            //  savedPaymentMethodType and passes it on to the PaymentsScreen.
            val paymentOption = paymentOptionsList.single { it.railCode == savedPaymentMethodType }
            checkIfPaymentOptionIsGPay(paymentOption)
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

    private fun checkIfPaymentOptionIsGPay(paymentMethodOption: PaymentMethodOption) {
        if (paymentMethodOption.railCode == GOOGLE_PAY) {
            val intent = Intent(requireActivity(), GooglePayActivity::class.java)
            val jsonString = Json.encodeToString(paymentMethodOption)
            intent.putExtra(GooglePayActivity.ARG_GPAY_PAYMENT_FIELDS, jsonString)
            intent.putExtra(ARG_ORDER_ID,orderId)
            startActivity(intent)
        } else {
            //  Navigate to payments screen to proceed with the selected payment option
            bundle.apply {
                putParcelable(ARG_PAYMENT_OPTION, paymentMethodOption as Parcelable)
                putString(ARG_ORDER_ID, orderId)
                //  In case of saved payment methods we need to pass payment method id.
                if (savedPaymentMethodId.isNotEmpty()) putString(
                    ARG_PAYMENT_METHOD_ID,
                    savedPaymentMethodId
                )
            }
            goToPaymentScreen()
        }
    }

    private fun onError(error: String) {
        (activity as HeadlessActivity).hideProgress()
        this.showAlert(error)
    }

    private fun goToPaymentScreen() {
        findNavController().navigate(
            R.id.action_payWithSavedPaymentOptions_to_payWithSavedPaymentMethod,
            bundle
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