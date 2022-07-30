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
import io.inai.android_sample_integration.*
import io.inai.android_sample_integration.Config.countryCode
import io.inai.android_sample_integration.helpers.NetworkRequestHandler
import io.inai.android_sample_integration.helpers.Orders.authenticationString
import io.inai.android_sample_integration.helpers.Orders.customerId
import io.inai.android_sample_integration.helpers.Orders.orderId
import io.inai.android_sample_integration.helpers.Orders.prepareOrder
import io.inai.android_sample_integration.helpers.json
import io.inai.android_sample_integration.helpers.showAlert
import io.inai.android_sample_integration.model.*
import kotlinx.android.synthetic.main.fragment_payment_options.*
import kotlinx.serialization.decodeFromString
import java.io.Serializable


class PaymentOptionsFragment : Fragment() {

    private lateinit var headlessOperation: HeadlessOperation
    private var savedPaymentMethodId = ""
    private var savedPaymentMethodType = ""
    private val inaiBackendPaymentOptionsUrl: String = BuildConfig.InaiBaseUrl + "payment-method-options"
    private val inaiBackendSavedPaymentMethod: String = BuildConfig.InaiBaseUrl + "customers/"
    private val paymentOptionsAdapter: PaymentOptionsAdapter by lazy { PaymentOptionsAdapter() }
    private val savedPaymentMethodsAdapter: SavedPaymentsMethodAdapter by lazy { SavedPaymentsMethodAdapter() }

    companion object {
        const val ARG_PAYMENT_OPTION = "arg_payment_option"
        const val ARG_PAYMENT_METHOD_ID = "arg-payment_method_id"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payment_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        headlessOperation = arguments?.getSerializable(HeadlessFragment.ARG_HEADLESS_OPERATION) as HeadlessOperation
        prepareUi()
        prepareOrder()
    }

    /**
     *  This functions checks for the current headless mode operation and initialozes the adapter click listeners and
     *  recycler views accordingly.
     */
    private fun prepareUi() {
        when (headlessOperation) {
            HeadlessOperation.MakePayment -> {
                ll_payment_options.visibility = View.VISIBLE
                ll_saved_payment_methods.visibility = View.GONE

                paymentOptionsAdapter.clickListener = { paymentMethodOption ->
                    //  Navigate to payments screen to proceed with the selected payment option
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

            HeadlessOperation.PayWithSavedPaymentMethod -> {
                ll_saved_payment_methods.visibility = View.VISIBLE
                ll_payment_options.visibility = View.GONE

                savedPaymentMethodsAdapter.clickListener = { paymentMethod ->
                    //  Store the payment method id of the selected saved payment to pass as
                    //  an argument to the payments screen
                    savedPaymentMethodId = paymentMethod.id ?: ""
                    //  Store the paymentMethodType of the selected saved payment method. This will be
                    //  used later in onSavedPaymentOptionsFetched() to filter out the payment fields
                    //  for that type
                    savedPaymentMethodType = paymentMethod.type ?: ""
                    //  Fetch payment method options for the selected saved payment method
                    //  by appending saved_payment_method=true param to the URL
                    fetchPaymentOptions(
                        "$inaiBackendPaymentOptionsUrl?order_id=$orderId&country=$countryCode&saved_payment_method=true"
                    )
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
        }
    }

    /**
     *  Prepares order with the appropriate call back for each headless mode.
     *  For MakePayment we fetch the available payment options once the order is prepared.
     *  For PayWithSavedPaymentMethod we fetch the saved payment methods once the order is
     *  prepared.
     */
    private fun prepareOrder() {
        when (headlessOperation) {
            HeadlessOperation.MakePayment -> prepareOrder {
                fetchPaymentOptions(
                    "$inaiBackendPaymentOptionsUrl?order_id=$orderId&country=$countryCode"
                )
            }
            HeadlessOperation.PayWithSavedPaymentMethod -> prepareOrder {
                fetchSavedPaymentMethods()
            }
        }
    }

    /**
     *  This function fetches the available payment options.
     *  In case od savePaymentMethods this fetches available payment options for the
     *  saved methods.
     *  When we get the API response onPaymentOptionsFetched() is called in case of MakePayment
     *  and onSavedPaymentOptionsFetched() is called in case of PayWithSavedPaymentMethod
     */
    private fun fetchPaymentOptions(
        urlWithParams: String
    ) {
        NetworkRequestHandler.makeGetRequest(
            urlWithParams,
            authenticationString
        ) { result: NetworkRequestHandler.Result ->
            when (result) {
                is NetworkRequestHandler.Result.Success -> {
                    when (headlessOperation) {
                        HeadlessOperation.MakePayment -> onPaymentOptionsFetched(result.message)
                        HeadlessOperation.PayWithSavedPaymentMethod -> onSavedPaymentOptionsFetched(result.message)
                    }
                }
                is NetworkRequestHandler.Result.Failure -> showAlert(result.message)
            }
        }
    }

    /**
     *  This function parses the payment options result, filters out "apple_pay" rail codes
     *  adds the list to the adapter.
     */
    private fun onPaymentOptionsFetched(response: String) {
        val paymentOptionsResult = json.decodeFromString<PaymentOptionsResult>(response)
        if (paymentOptionsResult.paymentMethodOptions.isNullOrEmpty()) {
            //  Show message payment options are not available
            showAlert("Payment options are not available")
        } else {
            // We do not need apple_pay to be shown on android app since apple pay will not work on android.
            val filteredList = paymentOptionsResult.paymentMethodOptions.filter {
                it.railCode != "apple_pay"
            }
            paymentOptionsAdapter.addList(filteredList)
        }
    }

    /**
     *  This function fetches the savedPaymentMethods.
     *  When we get the API response onSavedPaymentMethodsFetched() is called to handle the result.
     */
    private fun fetchSavedPaymentMethods() {
        val urlWithParams = "$inaiBackendSavedPaymentMethod$customerId/payment-methods"
        NetworkRequestHandler.makeGetRequest(
            urlWithParams,
            authenticationString
        ) { result ->
            when (result) {
                is NetworkRequestHandler.Result.Success -> onSavedPaymentMethodsFetched(result.message)
                is NetworkRequestHandler.Result.Failure -> showAlert(result.message)
            }
        }

    }


    /**
     *  This function parses the payment methods result, filters out "apple_pay" rail codes
     *  adds the list to the adapter.
     */
    private fun onSavedPaymentMethodsFetched(response: String) {
        val paymentsMethodsResult = json.decodeFromString<PaymentMethodsResult>(response)
        if (paymentsMethodsResult.paymentMethods.isNullOrEmpty()) {
            showAlert("Saved Payment Methods are not available")
        } else {
            // We do not need apple_pay to be shown on android app since apple pay will not work on android.
            val filteredList = (paymentsMethodsResult.paymentMethods).filter {
                it.type != "apple_pay"
            }
            savedPaymentMethodsAdapter.addList(filteredList)
        }
    }

    /**
     *  This method is called when fetch payment options for savedPaymentMethods.
     *  Parses the result and filters out the payment fields for the selected savedPaymentMethodType.
     */
    private fun onSavedPaymentOptionsFetched(response: String) {
        val paymentOptionsResult = json.decodeFromString<PaymentOptionsResult>(response)
        if (!paymentOptionsResult.paymentMethodOptions.isNullOrEmpty()) {
            //  Filters the paymentMethodOptions to get the payment fields for the selected
            //  savedPaymentMethodType and passes it on to the PaymentsScreen.
            goToPaymentScreen(
                paymentOptionsResult.paymentMethodOptions.single { it.railCode == savedPaymentMethodType }
            )
        }
    }

    /**
     *  This method:
     *      In case of MakePayment, passes only the selected payment option (which contain the payment fields)
     *      to the Payments screen.
     *      In case of PayWithSavedPaymentMethod, passes the payment option (which contain the payment fields)
     *      for the selected savedPaymentMethod along with the savedPaymentMethodId to the Payments screen.
     */
    private fun goToPaymentScreen(paymentMethodOption: PaymentMethodOption) {
        val bundle = Bundle()
        when (headlessOperation) {
            HeadlessOperation.MakePayment -> bundle.apply {
                putSerializable(ARG_PAYMENT_OPTION, paymentMethodOption as Serializable)
            }
            HeadlessOperation.PayWithSavedPaymentMethod -> bundle.apply {
                putSerializable(ARG_PAYMENT_OPTION, paymentMethodOption as Serializable)
                putString(ARG_PAYMENT_METHOD_ID, savedPaymentMethodId)
            }
        }
        findNavController().navigate(
            R.id.action_paymentOptionsFragment_to_paymentFieldsFragment,
            bundle
        )
    }
}