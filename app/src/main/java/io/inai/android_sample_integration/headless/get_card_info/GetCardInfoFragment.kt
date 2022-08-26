package io.inai.android_sample_integration.headless.get_card_info

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import io.inai.android_sample_integration.BuildConfig
import io.inai.android_sample_integration.Config
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.headless.HeadlessActivity
import io.inai.android_sample_integration.helpers.NetworkRequestHandler
import io.inai.android_sample_integration.helpers.json
import io.inai.android_sample_integration.helpers.showAlert
import io.inai.android_sdk.*
import kotlinx.android.synthetic.main.fragment_get_card_info.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.json.JSONObject

class GetCardInfoFragment : Fragment(R.layout.fragment_get_card_info), InaiCardInfoDelegate {

    private lateinit var editText: EditText
    private lateinit var textView: TextView
    private val inaiBackendOrdersUrl: String = BuildConfig.BaseUrl + "/orders"
    private val authenticationString = NetworkRequestHandler.getEncodedAuthString(Config.inaiToken, Config.inaiPassword)
    private var orderId = ""
    private val orderMetadata: Map<String, JsonPrimitive> = mutableMapOf(
        "test_order_id" to JsonPrimitive("test_order")
    )

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
        (activity as HeadlessActivity).showProgress()
        val orderPostData = getDataForOrders()
        val networkConfig = mutableMapOf(
            NetworkRequestHandler.KEY_URL to inaiBackendOrdersUrl,
            NetworkRequestHandler.KEY_REQUEST_TYPE to NetworkRequestHandler.POST,
            NetworkRequestHandler.KEY_AUTH_STRING to authenticationString,
            NetworkRequestHandler.KEY_POST_DATA_JSON to Json.encodeToString(orderPostData)
        )
        makeNetworkRequest(networkConfig, ::onOrderPrepared)
    }

    private fun onOrderPrepared(orderResponse: String) {
        val orderResult = json.decodeFromString<OrderResult>(orderResponse)
        Config.customerId = orderResult.customer_id
        orderId = orderResult.id

        getCardInfo(editText.text.toString())
    }

    private fun getCardInfo(cardNumber: String) {
        if (Config.inaiToken.isNotEmpty()) {
            val config = InaiConfig(
                token = Config.inaiToken,
                orderId = orderId,
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
        (activity as? HeadlessActivity)?.let{
            it.hideProgress()
            when (result.status) {
                InaiCardInfoStatus.Success -> {
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
    }

    /**
     *  Helper Functions
     */
    private fun makeNetworkRequest(
        networkConfig: Map<String, String>,
        callback: (String) -> Unit
    ) {
        NetworkRequestHandler.makeNetworkRequest(networkConfig) { result: NetworkRequestHandler.Result ->
            when (result) {
                is NetworkRequestHandler.Result.Success -> {
                    callback(result.message)
                }
                is NetworkRequestHandler.Result.Failure -> {
                    onError(result.message)
                }
            }
        }
    }

    private fun getDataForOrders(): OrderPostData {
        return OrderPostData(
            amount = Config.amount,
            currency = Config.currency,
            customer = OrderCustomer(
                email = "customer@example.com",
                first_name = "John",
                last_name = "Doe",
                contact_number = "01010101010",
                id = Config.customerId
            ),
            metadata = JsonObject(orderMetadata)
        )
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

    private fun onError(error: String) {
        (activity as HeadlessActivity).hideProgress()
        this.showAlert(error)
    }

    /**
     *  Fragment cycle callback.
     *  Here we cancel coroutine scope which in turn cancels any ongoing network operations
     */
    override fun onStop() {
        super.onStop()
        (activity as HeadlessActivity).hideProgress()
    }
}