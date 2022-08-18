package io.inai.android_sample_integration.headless.pay_with_saved_payment_method

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data classes related to SavedPayment Methods
 */

@Serializable
@Parcelize
data class PaymentMethodsResult(
    @SerialName("payment_methods")
    val paymentMethods: List<PaymentMethod>?
) : Parcelable


@Serializable
@Parcelize
data class PaymentMethod(
    @SerialName("card")
    val card: Card?,
    @SerialName("customer_id")
    val customerId: String?,
    @SerialName("id")
    val id: String?,
    @SerialName("type")
    val type: String?
) : Parcelable

@Serializable
@Parcelize
data class Card(
    @SerialName("brand")
    val brand: String? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("network")
    val network: String? = null,
    @SerialName("country")
    val country: String? = null,
    @SerialName("issuer")
    val issuer: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("expiry_month")
    val expiryMonth: Int? = null,
    @SerialName("expiry_year")
    val expiryYear: Int? = null,
    @SerialName("fingerprint")
    val fingerprint: String? = null,
    @SerialName("iin")
    val iin: String? = null,
    @SerialName("last_4")
    val last4: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("three_d_secure")
    val threeDSecure: String? = null
) : Parcelable