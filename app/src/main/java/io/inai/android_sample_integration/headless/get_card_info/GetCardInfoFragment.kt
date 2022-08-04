package io.inai.android_sample_integration.headless.get_card_info

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import io.inai.android_sample_integration.Config
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.headless.HeadlessActivity
import io.inai.android_sample_integration.helpers.Orders
import io.inai.android_sample_integration.helpers.showAlert
import io.inai.android_sdk.*
import kotlinx.android.synthetic.main.fragment_get_card_info.*
import org.json.JSONObject

class GetCardInfoFragment : Fragment(R.layout.fragment_get_card_info), InaiCardInfoDelegate {

    private lateinit var editText: EditText
    private lateinit var textView: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editText = view.findViewById(R.id.et_card_info)
        textView = view.findViewById(R.id.tv_card_info)

        btn_proceed.setOnClickListener {
            (activity as HeadlessActivity).showProgress()
            if (editText.text.isNotEmpty()) {
                prepareOrder()
            } else {
                requireContext().showAlert("Please enter a card number.")
            }
        }
    }

    private fun prepareOrder() {
        Orders.prepareOrder(requireContext().applicationContext) {
            getCardInfo(editText.text.toString())
        }
    }

    private fun getCardInfo(cardNumber: String) {
        if (Config.inaiToken.isNotEmpty()) {
            val config = InaiConfig(
                token = Config.inaiToken,
                orderId = Orders.orderId,
                countryCode = Config.countryCode
            )
            try {
                //  Check fields passed if they deserialize
                val inaiCheckout = InaiCheckout(config)
                inaiCheckout.getCardInfo(
                    cardNumber,
                    context = requireContext(),
                    delegate = this
                )
            } catch (ex: Exception) {
                //  Handle initialisation error
                requireContext().showAlert("Error while initialising sdk : $ex.message")
            }
        }
    }

    override fun cardInfoFetched(result: InaiCardInfoResult) {
        (activity as HeadlessActivity).hideProgress()
        when (result.status) {
            InaiCardInfoStatus.Success -> {
                Log.d("CARD_INFO", result.toString())
                //  Set boolean to to true.
                // Fetch logo based on card brand.
                val cardObject = result.data.get("card") as JSONObject
                val brandName = cardObject.get("brand") as String
                val type = cardObject.get("type") as String
                val country = cardObject.get("country") as String
                val organizationName = cardObject.get("issue_org_name") as String
                val organizationWebsite = cardObject.get("issue_org_website") as String
                val logo = getCardLogo(brandName)
                //  Display card details in text view
                textView.text = "$type\n$country\n$organizationName\n$organizationWebsite"
                // Display logo in the input field
                editText.setCompoundDrawablesWithIntrinsicBounds(null, null, logo, null)
            }
            InaiCardInfoStatus.Failed -> {
                requireContext().showAlert("Card Info Fail Result : ${result.data}")
            }
        }
    }


    //   Returns the respective image based on brand name.
    private fun getCardLogo(brandName: String): Drawable? {
        return when (brandName) {
            "VISA" -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_visa,
                null
            )
            "MASTERCARD" -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_mastercard,
                null
            )
            "AMERICAN EXPRESS" -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_amex,
                null
            )
            "DISCOVER" -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_discover,
                null
            )
            else -> ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_card_unknown,
                null
            )
        }
    }
}