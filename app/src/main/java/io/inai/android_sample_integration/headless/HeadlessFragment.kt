package io.inai.android_sample_integration.headless

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.navigation.fragment.findNavController
import io.inai.android_sample_integration.R
import kotlinx.android.synthetic.main.fragment_headless.*

enum class HeadlessOperation {
    MakePayment, PayWithSavedPaymentMethod
}


class HeadlessFragment : Fragment(R.layout.fragment_headless) {

    companion object {
        const val ARG_HEADLESS_OPERATION = "arg_payment_option"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_make_payment.setOnClickListener {
            findNavController().navigate(
                R.id.action_headlessFragment_to_checkoutFragment,
                Bundle().apply { putSerializable(ARG_HEADLESS_OPERATION, HeadlessOperation.MakePayment) }
            )
        }

        btn_save_payment_method.setOnClickListener {
            findNavController().navigate(R.id.action_headlessFragment_to_savePaymentMethodPaymentOptions)
        }

        btn_saved_payment_method.setOnClickListener {
            findNavController().navigate(
                R.id.action_headlessFragment_to_checkoutFragment,
                Bundle().apply { putSerializable(ARG_HEADLESS_OPERATION, HeadlessOperation.MakePayment) }
            )
        }

        btn_google_pay.setOnClickListener {
            findNavController().navigate(R.id.action_headlessFragment_to_googlePayPaymentOptions)
        }

        btn_validate_fields.setOnClickListener {
            findNavController().navigate(R.id.action_headlessFragment_to_validateFieldsPaymentOptionsFragment)
        }

        btn_get_card_info.setOnClickListener {
            findNavController().navigate(R.id.action_headlessFragment_to_getCardInfoFragment)
        }
    }
}