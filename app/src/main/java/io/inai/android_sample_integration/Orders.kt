package io.inai.android_sample_integration

import android.util.Log
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
    private val authenticationString = NetworkRequestHandler.getEncodedAuthString(inaiToken, inaiPassword)
    var orderId: String = ""
        private set
    var customerId = ""
        private set

    /***
     *  Order Creation
     */
    fun prepareOrder() {
        val orderPostData = getDataForOrders()
        val jsonString = Json.encodeToString(orderPostData)

        NetworkRequestHandler.makePostRequest(
            inaiBackendOrdersUrl,
            jsonString,
            authenticationString,
        ) { result: NetworkRequestHandler.Result ->
            when (result) {
                is NetworkRequestHandler.Result.Success -> onOrderPrepared(result.message)
                is NetworkRequestHandler.Result.Failure -> {/* Handle Failure Case */
                }
            }
        }
    }

    private fun onOrderPrepared(orderResponse: String) {
        val orderResult = json.decodeFromString<OrderResult>(orderResponse)
        customerId = orderResult.customer_id
        orderId = orderResult.id
        Log.d("ORDER","*******OrderId : $orderId*********")
        Log.d("ORDER","*******CustomerId : $customerId*********")
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
                    "test_order_id" to JsonPrimitive("test_order")
                )
            )
        )
    }
}