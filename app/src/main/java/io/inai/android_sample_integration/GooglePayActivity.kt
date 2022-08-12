package io.inai.android_sample_integration

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.WalletConstants
import io.inai.android_sample_integration.Config.countryCode
import io.inai.android_sample_integration.Config.inaiToken
import io.inai.android_sample_integration.helpers.Orders.orderId
import io.inai.android_sample_integration.helpers.showAlert
import io.inai.android_sdk.InaiCheckout
import io.inai.android_sdk.InaiConfig
import io.inai.android_sdk.InaiGooglePayRequestData
import kotlinx.android.synthetic.main.activity_google_pay.*
import org.json.JSONArray
import org.json.JSONObject

class GooglePayActivity : AppCompatActivity() {

    private lateinit var googlePayRequestData: InaiGooglePayRequestData
    private lateinit var paymentFields: JSONObject

    companion object {
        const val ARG_GPAY_PAYMENT_FIELDS = "gpay_payment_fields"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_pay)
        val jsonString = intent.getStringExtra(ARG_GPAY_PAYMENT_FIELDS) ?: ""
        paymentFields = JSONObject(jsonString)
        initGooglePay()
    }

    private fun initGooglePay() {
        // Convert data object into JSON object as required by SDK.
        val paymentMethodJSON = JSONObject()
        paymentMethodJSON.put("payment_method_options", JSONArray().put(paymentFields))

        //  Fire off google pay checks here..
        InaiCheckout.initGooglePay(
            paymentMethodJSON,
            WalletConstants.ENVIRONMENT_TEST,
            this,
        ) { googlePayRequestData ->
            run {
                if (googlePayRequestData != null) {
                    this.googlePayRequestData = googlePayRequestData
                    if (googlePayRequestData.canMakePayments) {
                        btn_google_pay.visibility = View.VISIBLE
                    } else {
                        this.showAlert("Google Pay Not Available")
                    }
                } else {
                    this.showAlert("Google Pay Not Available")
                }
            }
        }
    }

    fun googlePayButtonAction(@Suppress("UNUSED_PARAMETER") view: View) {
        initGooglePayRequest()
    }

    private val resolvePaymentForResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                RESULT_OK ->
                    result.data?.let { intent ->
                        PaymentData.getFromIntent(intent)?.let(::handleGooglePaySuccess)
                    }

                RESULT_CANCELED -> {
                    // The user canceled the payment attempt
                    showAlert("Google Pay Canceled")
                }
            }
        }


    private fun initGooglePayRequest() {
        val task = InaiCheckout.launchGooglePayRequest(this.googlePayRequestData)
        task.addOnCompleteListener { completedTask ->
            if (completedTask.isSuccessful) {
                completedTask.result.let(::handleGooglePaySuccess)
            } else {
                when (val exception = completedTask.exception) {
                    is ResolvableApiException -> {
                        resolvePaymentForResult.launch(
                            IntentSenderRequest.Builder(exception.resolution).build()
                        )
                    }
                    is ApiException -> {
                        val errorStr =
                            "ApiException Error code ${exception.statusCode} ${exception.message}"
                        showAlert(errorStr)
                    }
                    else -> {
                        val errorStr =
                            "Error ${CommonStatusCodes.INTERNAL_ERROR} Unexpected exception"
                        showAlert(errorStr)
                    }
                }
            }
        }
    }

    private fun handleGooglePaySuccess(paymentData: PaymentData) {
        //  Google Auth successful
        //  Init Inai SDK and process checkout
        try {
            //  Process google token for payment details data
            val paymentDetails = InaiCheckout.getGooglePayRequestData(paymentData)
            val config = InaiConfig(
                token = inaiToken,
                orderId = orderId,
                countryCode = countryCode,
            )
            val inaiCheckout = InaiCheckout(config)
            inaiCheckout.makePayment("google_pay", paymentDetails, this, this)
        } catch (ex: Exception) {
            //  Handle initialization error
            showAlert("Error while initializing sdk : $ex.message")
        }
    }


}