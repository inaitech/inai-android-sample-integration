package io.inai.android_sample_integration.headless

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.inai.android_sample_integration.Config
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.helpers.Orders
import io.inai.android_sample_integration.helpers.PaymentOptionsHelper
import io.inai.android_sample_integration.helpers.showAlert
import io.inai.android_sample_integration.model.PaymentMethodOption
import kotlinx.android.synthetic.main.fragment_save_payment_method_payment_options.*
import java.io.Serializable

class SavePaymentMethodPaymentOptions : Fragment() {

    private val paymentOptionsAdapter: PaymentOptionsAdapter by lazy { PaymentOptionsAdapter() }
    private val paymentOptionsHelper = PaymentOptionsHelper()
    private val bundle = Bundle()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_save_payment_method_payment_options, container, false)
    }

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
                putSerializable(PaymentOptionsFragment.ARG_PAYMENT_OPTION, paymentMethodOption as Serializable)
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
        val ordersCallback = { fetchPaymentOptions() }
        Orders.prepareOrder(ordersCallback)
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
            R.id.action_savePaymentMethodPaymentOptions_to_savePaymentMethod,
            bundle
        )
    }


}