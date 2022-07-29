package io.inai.android_sample_integration.headless

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.inai.android_sample_integration.*
import io.inai.android_sample_integration.Orders.authenticationString
import io.inai.android_sample_integration.Orders.orderId
import io.inai.android_sample_integration.Orders.prepareOrder
import io.inai.android_sample_integration.model.*
import kotlinx.android.synthetic.main.fragment_payment_options.*
import kotlinx.serialization.decodeFromString
import org.json.JSONObject
import java.io.Serializable


class PaymentOptionsFragment : Fragment() {

    private val inaiBackendPaymentOptionsUrl: String = BuildConfig.InaiBaseUrl + "payment-method-options"
    private val paymentOptionsAdapter: PaymentOptionsAdapter by lazy { PaymentOptionsAdapter() }

    companion object {
        const val ARG_PAYMENT_OPTION = "arg_payment_option"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payment_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareOrder {
            //  This callback will be called once the order is created successfully
            fetchPaymentOptions(orderId, Config.countryCode)
        }
        paymentOptionsAdapter.clickListener = { paymentMethodOption ->
            //  Navigate to payments screen
            goToPaymentScreen(paymentMethodOption)
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

    private fun fetchPaymentOptions(
        orderId: String,
        countryCode: String
    ) {
        val urlWithParams = "$inaiBackendPaymentOptionsUrl?order_id=$orderId&country=$countryCode"
        NetworkRequestHandler.makeGetRequest(
            urlWithParams,
            authenticationString
        ) { result: NetworkRequestHandler.Result ->
            when (result) {
                is NetworkRequestHandler.Result.Success -> onPaymentOptionsFetched(result.message)
                is NetworkRequestHandler.Result.Failure -> Log.d("ERROR","*********${result.message}*********")
            }
        }
    }

    private fun onPaymentOptionsFetched(response: String) {
        Log.d("JSON",JSONObject(response).toString(2))
        val paymentOptionsResult = json.decodeFromString<PaymentOptionsResult>(response)
        if (paymentOptionsResult.paymentMethodOptions.isNullOrEmpty()) {
            //  Show message payment options are not available
            showAlert("Payment options are not available")
        } else {
            // We do not need apple_pay to be shown on android app since apple pay will bot work on android.
            val filteredList = paymentOptionsResult.paymentMethodOptions.filter {
                it.railCode != "apple_pay"
            }
            showPaymentOptions(filteredList)
        }
    }

    private fun showPaymentOptions(paymentMethodOptions: List<PaymentMethodOption>) {
        ll_payment_options.visibility = View.VISIBLE
        paymentOptionsAdapter.addList(paymentMethodOptions)
    }

    private fun goToPaymentScreen(paymentMethodOption: PaymentMethodOption) {
        val bundle = Bundle().apply {
            putSerializable(ARG_PAYMENT_OPTION, paymentMethodOption as Serializable)
        }
        findNavController().navigate(
            R.id.action_paymentOptionsFragment_to_paymentFieldsFragment,
            bundle
        )
    }
}