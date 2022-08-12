package io.inai.android_sample_integration.headless.validate_fields

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.helpers.getSanitizedText

class PaymentOptionsAdapter() :
    RecyclerView.Adapter<PaymentOptionsAdapter.PaymentOptionsViewholder>() {

    private val paymentOptions: MutableList<PaymentMethodOption> = mutableListOf()
    var clickListener: (PaymentMethodOption) -> Unit = { }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentOptionsViewholder {
        return PaymentOptionsViewholder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_payment_options, parent, false)
        )
    }

    override fun onBindViewHolder(holder: PaymentOptionsViewholder, position: Int) {
        holder.setData(paymentOptions[position])
        holder.itemView.findViewById<RelativeLayout>(R.id.parent).setOnClickListener {
            clickListener(paymentOptions[position])
        }
    }

    override fun getItemCount(): Int {
        return paymentOptions.size
    }

    fun addList(list: List<PaymentMethodOption>) {
        paymentOptions.clear()
        paymentOptions.addAll(list)
        notifyItemRangeRemoved(0, list.size)
    }

    inner class PaymentOptionsViewholder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView by lazy { itemView.findViewById(R.id.txt_payment_option) }

        fun setData(item: PaymentMethodOption) {
            name.text = getSanitizedText(item.railCode ?: "")
        }
    }
}