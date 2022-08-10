package io.inai.android_sample_integration.headless.validate_fields

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.inai.android_sample_integration.Config
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.headless.HeadlessActivity
import io.inai.android_sample_integration.headless.make_payment.PaymentOptionsAdapter
import io.inai.android_sample_integration.headless.make_payment.PaymentOptionsFragment
import io.inai.android_sample_integration.helpers.NetworkRequestHandler
import io.inai.android_sample_integration.helpers.Orders
import io.inai.android_sample_integration.helpers.PaymentOptionsHelper
import io.inai.android_sample_integration.helpers.showAlert
import io.inai.android_sample_integration.model.PaymentMethodOption
import kotlinx.android.synthetic.main.fragment_payment_options.*
import java.io.Serializable

class ValidateFieldsPaymentOptions : Fragment(R.layout.fragment_validate_fields_payment_options) {

    private val paymentOptionsAdapter: PaymentOptionsAdapter by lazy { PaymentOptionsAdapter() }
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
        paymentOptionsAdapter.clickListener = { paymentMethodOption ->
            //  Navigate to validate screen to proceed with the selected payment option
            bundle.apply {
                putParcelable(PaymentOptionsFragment.ARG_PAYMENT_OPTION, paymentMethodOption as Parcelable)
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
        Orders.prepareOrder(requireContext().applicationContext ) { fetchPaymentOptions() }
    }

    private fun fetchPaymentOptions() {
        val queryParamMap = mapOf(
            PaymentOptionsFragment.PARAM_ORDER_ID to Orders.orderId,
            PaymentOptionsFragment.PARAM_COUNTRY_CODE to Config.countryCode
        )
        val paymentOptionsCallback = { paymentOptionsList: List<PaymentMethodOption> ->
            (activity as HeadlessActivity).hideProgress()
            // We do not need apple_pay to be shown on android app since apple pay will not work on android.
            val filteredList = paymentOptionsList.filter {
                it.railCode != PaymentOptionsFragment.APPLE_PAY
            }
            paymentOptionsAdapter.addList(filteredList)
        }

        //  This function parses the payment options result, filters out "apple_pay" rail codes
        //  adds the list to the adapter.
        paymentOptionsHelper.fetchPaymentOptions(queryParamMap, paymentOptionsCallback)
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