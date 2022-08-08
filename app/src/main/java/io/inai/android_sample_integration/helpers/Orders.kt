package io.inai.android_sample_integration.helpers

import android.content.Context
import io.inai.android_sample_integration.BuildConfig
import io.inai.android_sample_integration.Config
import io.inai.android_sample_integration.Config.amount
import io.inai.android_sample_integration.Config.customerId
import io.inai.android_sample_integration.Config.inaiPassword
import io.inai.android_sample_integration.Config.inaiToken
import io.inai.android_sample_integration.model.OrderCustomer
import io.inai.android_sample_integration.model.OrderPostData
import io.inai.android_sample_integration.model.OrderResult
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object Orders {

    private const val inaiBackendOrdersUrl: String = BuildConfig.InaiBaseUrl + "/orders"
    val authenticationString
        get() = NetworkRequestHandler.getEncodedAuthString(inaiToken, inaiPassword)
    //  A map that can be added to a JSON object as metadata for Order creation.
    //  Any key,value pair can be added to this map before creating an order.
    val metadata : Map<String,JsonPrimitive> = mutableMapOf(
        "test_order_id" to JsonPrimitive("test_order"),
        "vat" to JsonPrimitive("6"),
        "tax_percentage" to JsonPrimitive("12"),
        "taxable_amount" to JsonPrimitive("50")
    )
    var orderId: String = ""
        private set


    /***
     *  Order Creation
     */
    fun prepareOrder(applicationContext: Context, callback: () -> Unit) {
        val orderPostData = getDataForOrders()
        val jsonString = Json.encodeToString(orderPostData)

        NetworkRequestHandler.makePostRequest(
            inaiBackendOrdersUrl,
            jsonString,
            authenticationString,
        ) { result: NetworkRequestHandler.Result ->
            when (result) {
                is NetworkRequestHandler.Result.Success -> {
                    onOrderPrepared(result.message)
                    callback()
                }
                is NetworkRequestHandler.Result.Failure -> {
                    applicationContext.showAlert(result.message)
                }
            }
        }
    }

    private fun onOrderPrepared(orderResponse: String) {
        val orderResult = json.decodeFromString<OrderResult>(orderResponse)
        customerId = orderResult.customer_id
        orderId = orderResult.id
    }

    private fun getDataForOrders(): OrderPostData {
        return OrderPostData(
            amount = amount,
            currency = Config.currency,
            customer = OrderCustomer(
                email = "testdev@inai.io",
                first_name = "Dev",
                last_name = "Smith",
                contact_number = "01010101010",
                id = customerId
            ),
            metadata = JsonObject(metadata)
        )
    }
}