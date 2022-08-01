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
import io.inai.android_sample_integration.Config.customerId
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

    private lateinit var headlessOperation: HeadlessOperation
    private var savedPaymentMethodId = ""
    private var savedPaymentMethodType = ""
    private val paymentOptionsAdapter: PaymentOptionsAdapter by lazy { PaymentOptionsAdapter() }
    private val savedPaymentMethodsAdapter: SavedPaymentsMethodAdapter by lazy { SavedPaymentsMethodAdapter() }
    private val paymentOptionsHelper = PaymentOptionsHelper()
    private val bundle = Bundle()

    companion object {
        const val ARG_PAYMENT_OPTION = "arg_payment_option"
        const val ARG_PAYMENT_METHOD_ID = "arg-payment_method_id"
        const val PARAM_ORDER_ID = "order_id"
        const val PARAM_COUNTRY_CODE = "country"
        const val PARAM_SAVED_PAYMENT_METHOD = "saved_payment_method"
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
        paymentOptionsHelper.errorCallback = { error ->
            showAlert(error)
        }
        prepareUi()
        prepareOrder()
    }

    /**
     *  This functions checks for the current headless mode operation and initializes the adapter click listeners and
     *  recycler views accordingly.
     */
    private fun prepareUi() {
        when (headlessOperation) {
            HeadlessOperation.MakePayment -> {
                ll_payment_options.visibility = View.VISIBLE
                ll_saved_payment_methods.visibility = View.GONE

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

            HeadlessOperation.PayWithSavedPaymentMethod -> {
                ll_saved_payment_methods.visibility = View.VISIBLE
                ll_payment_options.visibility = View.GONE

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
                        PARAM_ORDER_ID to orderId,
                        PARAM_COUNTRY_CODE to countryCode,
                        PARAM_SAVED_PAYMENT_METHOD to "true"
                    )
                    (activity as HeadlessActivity).showProgress()
                    paymentOptionsHelper.fetchPaymentOptions(queryParamMap) { paymentOptionsList ->
                        (activity as HeadlessActivity).hideProgress()
                        //  Filters the paymentMethodOptions to get the payment fields for the selected
                        //  savedPaymentMethodType and passes it on to the PaymentsScreen.
                        val paymentOption = paymentOptionsList.single { it.railCode == savedPaymentMethodType }
                        checkIfPaymentOptionIsGPay(paymentOption)
                    }
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
        (activity as HeadlessActivity).showProgress()
        when (headlessOperation) {
            HeadlessOperation.MakePayment -> prepareOrder {
                val queryParamMap = mapOf(
                    PARAM_ORDER_ID to orderId,
                    PARAM_COUNTRY_CODE to countryCode
                )
                //  This function parses the payment options result, filters out "apple_pay" rail codes
                //  adds the list to the adapter.
                paymentOptionsHelper.fetchPaymentOptions(queryParamMap) { paymentOptionsList ->
                    (activity as HeadlessActivity).hideProgress()
                    // We do not need apple_pay to be shown on android app since apple pay will not work on android.
                    val filteredList = paymentOptionsList.filter {
                        it.railCode != "apple_pay"
                    }
                    paymentOptionsAdapter.addList(filteredList)
                }
            }
            HeadlessOperation.PayWithSavedPaymentMethod -> prepareOrder {
                //  This function parses the payment methods result, filters out "apple_pay" rail codes
                //   adds the list to the adapter.
                paymentOptionsHelper.fetchSavedPaymentMethods(customerId) { paymentMethodsList ->
                    (activity as HeadlessActivity).hideProgress()
                    val filteredList = paymentMethodsList.filter {
                        it.type != "apple_pay"
                    }
                    savedPaymentMethodsAdapter.addList(filteredList)
                }
            }
        }
    }

    private fun checkIfPaymentOptionIsGPay(paymentMethodOption: PaymentMethodOption) {
        if (paymentMethodOption.railCode == "google_pay") {
            val intent = Intent(requireActivity(), GooglePayActivity::class.java)
            val jsonString = Json.encodeToString(paymentMethodOption)
            intent.putExtra(GooglePayActivity.ARG_GPAY_PAYMENT_FIELDS,jsonString)
            startActivity(intent)
        } else {
            //  Navigate to payments screen to proceed with the selected payment option
            bundle.apply {
                putSerializable(ARG_PAYMENT_OPTION, paymentMethodOption as Serializable)
                //  In case of saved payment methods we need to pass payment method id.
                if (savedPaymentMethodId.isNotEmpty()) putString(ARG_PAYMENT_METHOD_ID, savedPaymentMethodId)
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