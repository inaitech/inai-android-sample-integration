package io.inai.android_sample_integration.headless

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.inai.android_sample_integration.*
import io.inai.android_sample_integration.Config.countryCode
import io.inai.android_sample_integration.helpers.Orders.orderId
import io.inai.android_sample_integration.helpers.Orders.prepareOrder
import io.inai.android_sample_integration.helpers.PaymentOptionsHelper
import io.inai.android_sample_integration.helpers.showAlert
import io.inai.android_sample_integration.model.PaymentMethodOption
import kotlinx.android.synthetic.main.fragment_payment_options.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.Serializable


class PaymentOptionsFragment : Fragment() {

    private val paymentOptionsAdapter: PaymentOptionsAdapter by lazy { PaymentOptionsAdapter() }
    private val paymentOptionsHelper = PaymentOptionsHelper()
    private val bundle = Bundle()

    companion object {
        const val ARG_PAYMENT_OPTION = "arg_payment_option"
        const val ARG_PAYMENT_METHOD_ID = "arg-payment_method_id"
        const val PARAM_ORDER_ID = "order_id"
        const val PARAM_COUNTRY_CODE = "country"
        const val PARAM_SAVED_PAYMENT_METHOD = "saved_payment_method"
        const val APPLE_PAY = "apple_pay"
        const val GOOGLE_PAY = "google_pay"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payment_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        paymentOptionsHelper.errorCallback = { error ->
            showAlert(error)
        }
        prepareUi()
        prepareOrder()
    }

    private fun prepareUi() {
        paymentOptionsAdapter.clickListener = { paymentMethodOption ->
            checkIfPaymentOptionIsGPay(paymentMethodOption)
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
        prepareOrder { fetchPaymentOptions() }
    }

    private fun fetchPaymentOptions() {
        val queryParamMap = mapOf(
            PARAM_ORDER_ID to orderId,
            PARAM_COUNTRY_CODE to countryCode
        )
        val paymentOptionsCallback = { paymentOptionsList: List<PaymentMethodOption> ->
            (activity as HeadlessActivity).hideProgress()
            // We do not need apple_pay to be shown on android app since apple pay will not work on android.
            val filteredList = paymentOptionsList.filter {
                it.railCode != APPLE_PAY
            }
            paymentOptionsAdapter.addList(filteredList)
        }

        //  This function parses the payment options result, filters out "apple_pay" rail codes
        //  adds the list to the adapter.
        paymentOptionsHelper.fetchPaymentOptions(queryParamMap, paymentOptionsCallback)
    }

    private fun checkIfPaymentOptionIsGPay(paymentMethodOption: PaymentMethodOption) {
        if (paymentMethodOption.railCode == GOOGLE_PAY) {
            val intent = Intent(requireActivity(), GooglePayActivity::class.java)
            val jsonString = Json.encodeToString(paymentMethodOption)
            intent.putExtra(GooglePayActivity.ARG_GPAY_PAYMENT_FIELDS, jsonString)
            startActivity(intent)
        } else {
            //  Navigate to payments screen to proceed with the selected payment option
            bundle.apply {
                putSerializable(ARG_PAYMENT_OPTION, paymentMethodOption as Serializable)
            }
            goToPaymentScreen()
        }
    }

    private fun goToPaymentScreen() {
        findNavController().navigate(
            R.id.action_paymentOptionsFragment_to_paymentFieldsFragment,
            bundle
        )
    }
}