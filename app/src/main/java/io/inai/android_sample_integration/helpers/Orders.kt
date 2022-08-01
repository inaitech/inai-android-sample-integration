package io.inai.android_sample_integration.helpers

import io.inai.android_sample_integration.BuildConfig
import io.inai.android_sample_integration.Config
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

    private const val inaiBackendOrdersUrl: String = BuildConfig.InaiBaseUrl + "orders"
    val authenticationString
        get() = NetworkRequestHandler.getEncodedAuthString(inaiToken, inaiPassword)
    var orderId: String = ""
        private set

    /***
     *  Order Creation
     */
    fun prepareOrder(callback: () -> Unit) {
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
            amount = "100",
            currency = Config.currency,
            customer = OrderCustomer(
                email = "testdev@inai.io",
                first_name = "Dev",
                last_name = "Smith",
                contact_number = "01010101010",
                id = customerId
            ),
            metadata = JsonObject(
                mapOf(
                    "test_order_id" to JsonPrimitive("test_order"),
                    "vat" to JsonPrimitive("6"),
                    "tax_percentage" to JsonPrimitive("12"),
                    "taxable_amount" to JsonPrimitive("50")
                )
            )
        )
    }
}