package io.inai.android_sample_integration.headless

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.navigation.fragment.findNavController
import io.inai.android_sample_integration.MainActivity.Companion.ARG_PAYMENT_OPERATION
import io.inai.android_sample_integration.PaymentOperation
import io.inai.android_sample_integration.R
import kotlinx.android.synthetic.main.fragment_checkout.*

class ProductCheckoutFragment : Fragment(R.layout.fragment_checkout) {

    private lateinit var paymentOperation: PaymentOperation

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        paymentOperation = arguments?.getSerializable(ARG_PAYMENT_OPERATION) as PaymentOperation
        btn_buy.setOnClickListener {
            when (paymentOperation) {
                PaymentOperation.MakePayment -> findNavController().navigate(
                    R.id.action_checkoutFragment_to_paymentOptionsFragment
                )
                PaymentOperation.PayWithSavedPaymentMethod -> findNavController().navigate(
                    R.id.action_checkoutFragment_to_payWithSavedPaymentOptions
                )
            }
        }
    }

}