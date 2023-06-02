package io.inai.android_sample_integration.headless.make_payment

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.inai.android_sample_integration.*
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.BuildConfig
import io.inai.android_sample_integration.Config.amount
import io.inai.android_sample_integration.Config.countryCode
import io.inai.android_sample_integration.Config.currency
import io.inai.android_sample_integration.google_pay.GooglePayActivity
import io.inai.android_sample_integration.headless.HeadlessActivity
import io.inai.android_sample_integration.helpers.*
import io.inai.android_sdk.*
import kotlinx.android.synthetic.main.fragment_make_payment_payment_options.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.json.JSONObject


class MakePayment_PaymentOptionsFragment : Fragment(R.layout.fragment_make_payment_payment_options) ,InaiCheckoutDelegate{

    private val inaiBackendOrdersUrl: String = BuildConfig.BaseUrl + "/orders"
    private val inaiBackendPaymentOptionsUrl: String = BuildConfig.BaseUrl + "/payment-method-options"
    private var orderId = ""
    private val orderMetadata: Map<String, JsonPrimitive> = mutableMapOf(
        "test_order_id" to JsonPrimitive("test_order")
    )
   // private val paymentOptionsAdapter: PaymentOptionsAdapter by lazy { PaymentOptionsAdapter() }
    private val paymentOptionsAdapter: PaymentOptionsAdapterNew by lazy { PaymentOptionsAdapterNew() }

    private val bundle = Bundle()

    companion object {
        const val ARG_ORDER_ID = "order_id"
        const val ARG_PAYMENT_OPTION = "arg_payment_option"
        const val APPLE_PAY = "apple_pay"
        const val GOOGLE_PAY = "google_pay"
        lateinit var PaymentMethodOptionsList: List<PaymentMethodOption>

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareUi()
        prepareOrder()
    }

    private fun prepareUi() {
        paymentOptionsAdapter.clickListener = { paymentMethodOption ->
            checkIfPaymentOptionIsGPay(paymentMethodOption)
        }
        paymentOptionsAdapter.payBtnClickListener = { paymentDetails: JSONObject, paymentMethodOption: PaymentMethodOption ->
            makePayment(paymentDetails, paymentMethodOption)
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
        val url = "$inaiBackendPaymentOptionsUrl?order_id=$orderId&country=$countryCode"
        val networkConfig = mutableMapOf(
            NetworkRequestHandler.KEY_URL to url,
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
            PaymentMethodOptionsList = filteredList

            var list = mutableListOf<PaymentMethodOption>()
            list.addAll(filteredList)

            var walletPaymentOptions = filteredList.filter { it.category == Constants.CATEGORY_WALLET } as MutableList<PaymentMethodOption>
            if (walletPaymentOptions.isNotEmpty()){
                list.removeAll(walletPaymentOptions)
                list.add(walletPaymentOptions[0])
            }
            paymentOptionsAdapter.addList(list)
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
            amount = amount,
            currency = currency,
            customer = OrderCustomer(
                email = "customer@example.com",
                first_name = "John",
                last_name = "Doe",
                contact_number = "9600099009",
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
            //removing the upi_intent form fields since we already displayed upi_intent in the payment-options page
            if (paymentMethodOption.railCode == Constants.RAIL_CODE_UPI) {
                val filteredList = paymentMethodOption.modes?.filter {
                    it.code != Constants.MODE_CODE_UPI_INTENT
                }
                paymentMethodOption.modes = filteredList
            }

            //  Navigate to payments screen to proceed with the selected payment option
            bundle.apply {
                putString(ARG_ORDER_ID, orderId)
                putParcelable(ARG_PAYMENT_OPTION, paymentMethodOption as Parcelable)
            }
            goToPaymentScreen()
        }
    }

    private fun makePayment(paymentDetails: JSONObject, paymentMethodOption: PaymentMethodOption) {
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


    private fun onError(error: String) {
        (activity as HeadlessActivity).hideProgress()
        this.showAlert(error)
    }

    private fun goToPaymentScreen() {
        findNavController().navigate(
            R.id.action_paymentOptionsFragment_to_paymentFieldsFragment,
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