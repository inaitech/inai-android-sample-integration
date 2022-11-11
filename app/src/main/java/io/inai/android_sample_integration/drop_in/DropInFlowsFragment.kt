package io.inai.android_sample_integration.drop_in

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.navigation.fragment.findNavController
import io.inai.android_sample_integration.R
import kotlinx.android.synthetic.main.fragment_drop_in_flows.*


class DropInFlowsFragment : Fragment(R.layout.fragment_drop_in_flows) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_checkout.setOnClickListener {
            findNavController().navigate(R.id.action_dropInFlowsFragment_to_presentCheckoutFragment)
        }

        btn_add_payment_method.setOnClickListener {
            findNavController().navigate(R.id.action_dropInFlowsFragment_to_addPaymentMethodFragment)
        }

        btn_pay_with_payment_method.setOnClickListener {
            findNavController().navigate(R.id.action_dropInFlowsFragment_to_payWithPaymentMethodFragment)
        }

    }
}