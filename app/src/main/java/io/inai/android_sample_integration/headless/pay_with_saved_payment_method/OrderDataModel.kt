package io.inai.android_sample_integration.headless.pay_with_saved_payment_method

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class OrderResult(val uuid: String, val id: String, val customer_id: String)

@Serializable
data class OrderCustomer(
    val email: String,
    val first_name: String,
    val last_name: String,
    var contact_number: String? = null,
    var id: String? = null,
    var external_id: String? = null,
    var phone: String? = null
)

@Serializable
data class OrderPostData(
    val amount: String,
    val currency: String,
    val customer: OrderCustomer? = null,
    val metadata: JsonElement? = null
)