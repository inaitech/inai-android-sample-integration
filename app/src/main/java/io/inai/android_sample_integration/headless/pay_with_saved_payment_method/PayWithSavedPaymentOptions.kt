package io.inai.android_sample_integration.headless.pay_with_saved_payment_method

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.inai.android_sample_integration.Config
import io.inai.android_sample_integration.google_pay.GooglePayActivity
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.headless.HeadlessActivity
import io.inai.android_sample_integration.headless.make_payment.PaymentOptionsFragment
import io.inai.android_sample_integration.helpers.Orders
import io.inai.android_sample_integration.helpers.PaymentOptionsHelper
import io.inai.android_sample_integration.helpers.showAlert
import io.inai.android_sample_integration.model.PaymentMethod
import io.inai.android_sample_integration.model.PaymentMethodOption
import kotlinx.android.synthetic.main.fragment_pay_with_saved_payment_options.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.Serializable

class PayWithSavedPaymentOptions : Fragment(R.layout.fragment_pay_with_saved_payment_options) {

    private var savedPaymentMethodId = ""
    private var savedPaymentMethodType = ""
    private val savedPaymentMethodsAdapter: SavedPaymentsMethodAdapter by lazy { SavedPaymentsMethodAdapter() }
    private val paymentOptionsHelper = PaymentOptionsHelper()
    private val bundle = Bundle()

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
        savedPaymentMethodsAdapter.clickListener = { paymentMethod ->
            //  Store the payment method id of the selected saved payment to pass as
            //  an argument to the payments screen
            savedPaymentMethodId = paymentMethod.id ?: ""
            //  Store the paymentMethodType of the selected saved payment method. This will be
            //  used later to filter out the payment fields for that type
            savedPaymentMethodType = paymentMethod.type ?: ""
            //  Fetch payment method options for the selected saved payment method
            //  by adding saved_payment_method=true param to query map.
            val queryParamMap = mapOf(
                PaymentOptionsFragment.PARAM_ORDER_ID to Orders.orderId,
                PaymentOptionsFragment.PARAM_COUNTRY_CODE to Config.countryCode,
                PaymentOptionsFragment.PARAM_SAVED_PAYMENT_METHOD to "true"
            )
            //  Callback that specifies what to do after fetching payment options.
            val paymentOptionsCallback = { paymentOptionsList: List<PaymentMethodOption> ->
                (activity as HeadlessActivity).hideProgress()
                //  Filters the paymentMethodOptions to get the payment fields for the selected
                //  savedPaymentMethodType and passes it on to the PaymentsScreen.
                val paymentOption = paymentOptionsList.single { it.railCode == savedPaymentMethodType }
                checkIfPaymentOptionIsGPay(paymentOption)
            }

            (activity as HeadlessActivity).showProgress()
            paymentOptionsHelper.fetchPaymentOptions(queryParamMap, paymentOptionsCallback)
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
        Orders.prepareOrder(requireContext().applicationContext) { fetchSavedPaymentMethods() }
    }

    private fun fetchSavedPaymentMethods() {
        //  Callback that specifies what should be done after saved payment methods are fetched.
        //  This callback parses the payment methods result, filters out "apple_pay" rail codes
        //   adds the list to the adapter.
        val savedPaymentMethodCallback = { paymentMethodsList: List<PaymentMethod> ->
            (activity as HeadlessActivity).hideProgress()
            val filteredList = paymentMethodsList.filter {
                it.type != PaymentOptionsFragment.APPLE_PAY
            }
            savedPaymentMethodsAdapter.addList(filteredList)
        }

        paymentOptionsHelper.fetchSavedPaymentMethods(Config.customerId, savedPaymentMethodCallback)
    }

    private fun checkIfPaymentOptionIsGPay(paymentMethodOption: PaymentMethodOption) {
        if (paymentMethodOption.railCode == PaymentOptionsFragment.GOOGLE_PAY) {
            val intent = Intent(requireActivity(), GooglePayActivity::class.java)
            val jsonString = Json.encodeToString(paymentMethodOption)
            intent.putExtra(GooglePayActivity.ARG_GPAY_PAYMENT_FIELDS, jsonString)
            startActivity(intent)
        } else {
            //  Navigate to payments screen to proceed with the selected payment option
            bundle.apply {
                putSerializable(PaymentOptionsFragment.ARG_PAYMENT_OPTION, paymentMethodOption as Serializable)
                //  In case of saved payment methods we need to pass payment method id.
                if (savedPaymentMethodId.isNotEmpty()) putString(
                    PaymentOptionsFragment.ARG_PAYMENT_METHOD_ID,
                    savedPaymentMethodId
                )
            }
            goToPaymentScreen()
        }
    }

    private fun goToPaymentScreen() {
        findNavController().navigate(
            R.id.action_payWithSavedPaymentOptions_to_payWithSavedPaymentMethod,
            bundle
        )
    }
}