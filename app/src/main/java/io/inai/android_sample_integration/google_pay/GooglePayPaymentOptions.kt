package io.inai.android_sample_integration.google_pay

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.inai.android_sample_integration.Config
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.headless.HeadlessActivity
import io.inai.android_sample_integration.headless.make_payment.PaymentMethodOption
import io.inai.android_sample_integration.headless.make_payment.PaymentOptionsAdapter
import io.inai.android_sample_integration.headless.make_payment.MakePayment_PaymentOptionsFragment
import io.inai.android_sample_integration.helpers.NetworkRequestHandler
import io.inai.android_sample_integration.helpers.Orders
import io.inai.android_sample_integration.helpers.PaymentOptionsHelper
import io.inai.android_sample_integration.helpers.showAlert
import kotlinx.android.synthetic.main.fragment_google_pay_payment_options.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GooglePayPaymentOptions : Fragment(R.layout.fragment_google_pay_payment_options) {

    private val paymentOptionsAdapter: PaymentOptionsAdapter by lazy { PaymentOptionsAdapter() }
    private val paymentOptionsHelper = PaymentOptionsHelper()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        paymentOptionsHelper.errorCallback = { error ->
            (activity as HeadlessActivity).hideProgress()
            showAlert(error)
        }
        prepareUi()
        prepareOrder()
    }

    private fun prepareUi() {
        paymentOptionsAdapter.clickListener = { paymentMethodOption ->
           // Start Google Pay activity
            val intent = Intent(requireActivity(), GooglePayActivity::class.java)
            val jsonString = Json.encodeToString(paymentMethodOption)
            intent.putExtra(GooglePayActivity.ARG_GPAY_PAYMENT_FIELDS, jsonString)
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
        Orders.prepareOrder(requireContext()) { fetchPaymentOptions() }
    }

    private fun fetchPaymentOptions() {
        val queryParamMap = mapOf(
            MakePayment_PaymentOptionsFragment.PARAM_ORDER_ID to Orders.orderId,
            MakePayment_PaymentOptionsFragment.PARAM_COUNTRY_CODE to Config.countryCode
        )
        val paymentOptionsCallback = { paymentOptionsList: List<PaymentMethodOption> ->
            (activity as HeadlessActivity).hideProgress()
            // We only need to show google pay option here.
            val filteredList = paymentOptionsList.filter {
                it.railCode == MakePayment_PaymentOptionsFragment.GOOGLE_PAY
            }
            paymentOptionsAdapter.addList(filteredList)
        }

        //  This function parses the payment options result, filters out "apple_pay" rail codes
        //  adds the list to the adapter.
        paymentOptionsHelper.fetchPaymentOptions(queryParamMap, paymentOptionsCallback)
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