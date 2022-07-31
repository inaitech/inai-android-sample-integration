package io.inai.android_sample_integration.helpers

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.core.content.res.ResourcesCompat
import io.inai.android_sample_integration.Config.countryCode
import io.inai.android_sample_integration.Config.inaiToken
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.helpers.Orders.orderId
import io.inai.android_sdk.*
import org.json.JSONObject

/**
 *  An inner class which implements both the TextWatcher and InaiCardInfoDelegate interfaces
 *  - Monitor card number entry.
 *  - Verify card number and fetch details of card.
 *  - Display brand logo.
 */
class CardInfoHelper(private val editText: EditText, private val context: Context) : TextWatcher, InaiCardInfoDelegate {
    //  Internal boolean which is set to true once cardInfo is fetched.
    //  This prevents multiple api calls on consequent user input.
    private var isCardInfoFetched = false

    override fun beforeTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {

    }

    override fun onTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {

    }

    override fun afterTextChanged(charSequence: Editable?) {
        charSequence?.let {
            //  Once the length of card number input exceeds 6 characters and isCardInfoFetched
            //  is false, we call getCardInfo() Api to check the card brand.
            if (charSequence.length >= 6 && !isCardInfoFetched) {
                getCardInfo(charSequence.toString())
            } else if (charSequence.length < 6) {
                //  Remove any existing drawable on edit text.
                isCardInfoFetched = false
                editText.setCompoundDrawables(null, null, null, null)
            }
        }
    }

    override fun cardInfoFetched(result: InaiCardInfoResult) {
        when (result.status) {
            InaiCardInfoStatus.Success -> {
                //  Set boolean to to true.
                isCardInfoFetched = true
                // Fetch logo based on card brand.
                val cardObject = result.data.get("card") as JSONObject
                val brandName = cardObject.get("brand").toString()
                val logo = getCardLogo(brandName)
                // Display logo in the input field
                editText.setCompoundDrawablesWithIntrinsicBounds(null, null, logo, null)
            }
            InaiCardInfoStatus.Failed -> {
                context.showAlert("Card Info Fail Result : ${result.data}")
            }
        }
    }

    private fun getCardInfo(cardNumber: String) {
        if (inaiToken.isNotEmpty()) {
            val config = InaiConfig(
                token = inaiToken,
                orderId = orderId,
                countryCode = countryCode
            )
            try {
                //  Check fields passed if they deserialize
                val inaiCheckout = InaiCheckout(config)
                inaiCheckout.getCardInfo(
                    cardNumber,
                    context = context,
                    delegate = this
                )
            } catch (ex: Exception) {
                //  Handle initialisation error
                context.showAlert("Error while initialising sdk : $ex.message")
            }
        }

    }

    //   Returns the respective image based on brand name.
    private fun getCardLogo(brandName: String): Drawable? {
        return when (brandName) {
            "VISA" -> ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_visa,
                null
            )
            "MASTERCARD" -> ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_mastercard,
                null
            )
            "AMERICAN EXPRESS" -> ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_amex,
                null
            )
            "DISCOVER" -> ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_discover,
                null
            )
            else -> ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_card_unknown,
                null
            )
        }
    }
}