package io.inai.android_sample_integration.headless

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.inai.android_sample_integration.R
import kotlinx.android.synthetic.main.fragment_checkout.*

enum class HeadlessOperation() {
    MakePayment, PayWithSavedPaymentMethod
}

class CheckoutFragment : Fragment() {

    private lateinit var headlessOperation: HeadlessOperation

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_checkout, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        headlessOperation = arguments?.getSerializable(HeadlessFragment.ARG_HEADLESS_OPERATION) as HeadlessOperation
        btn_buy.setOnClickListener {
            when (headlessOperation) {
                HeadlessOperation.MakePayment -> findNavController().navigate(
                    R.id.action_checkoutFragment_to_paymentOptionsFragment
                )
                HeadlessOperation.PayWithSavedPaymentMethod -> findNavController().navigate(
                    R.id.action_checkoutFragment_to_payWithSavedPaymentOptions
                )
            }
        }
    }

}