package io.inai.android_sample_integration.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.json.JSONObject


/**
 * Data classes related to Payment Options
 */
@Serializable
data class PaymentOptionsResult(
    @SerialName("payment_method_options")
    val paymentMethodOptions: List<PaymentMethodOption>?
)

@Serializable
data class PaymentMethodOption(
    @SerialName("configs")
    val configs: JsonElement? = null,
    @SerialName("form_fields")
    val formFields: List<FormField>,
    @SerialName("rail_code")
    val railCode: String,
) : java.io.Serializable

@Serializable
data class FormField(
    @SerialName("field_type")
    val fieldType: String,
    @SerialName("label")
    val label: String,
    @SerialName("name")
    val name: String,
    @SerialName("placeholder")
    val placeholder: String? = null,
    @SerialName("required")
    val required: Boolean,
    @SerialName("validations")
    val validations: Validations,
    @SerialName("data")
    val data: Data? = null
)

@Serializable
data class SupportedNetwork(
    @SerialName("name")
    val name: String? = null
)


@Serializable
data class Tokenization(
    @SerialName("parameters")
    val parameters: Parameters? = null,
    @SerialName("type")
    val type: String? = null
)

@Serializable
data class Parameters(
    @SerialName("protocolVersion")
    val protocolVersion: String? = null,
    @SerialName("publicKey")
    val publicKey: String? = null
)

@Serializable
data class Validations(
    var input_mask_regex: String? = null,
    var max_length: Int? = null,
    var min_length: Int? = null,
    var pattern: String? = null,
    var required: Boolean? = null
)

@Serializable
data class Data(
    @SerialName("values")
    var values: List<Values>? = null
)

@Serializable
data class Values(
    val label: String,
    val value: String
)


