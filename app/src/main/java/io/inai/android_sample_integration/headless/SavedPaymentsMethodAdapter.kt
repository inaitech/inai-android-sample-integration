package io.inai.android_sample_integration.headless

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.helpers.getSanitizedText
import io.inai.android_sample_integration.model.PaymentMethod

class SavedPaymentsMethodAdapter() :
    RecyclerView.Adapter<SavedPaymentsMethodAdapter.PaymentMethodsViewholder>() {

    private val paymentMethods: MutableList<PaymentMethod> = mutableListOf()
    var clickListener: (PaymentMethod) -> Unit = { }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SavedPaymentsMethodAdapter.PaymentMethodsViewholder {
        return PaymentMethodsViewholder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_payment_options, parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: SavedPaymentsMethodAdapter.PaymentMethodsViewholder,
        position: Int
    ) {
        holder.setData(paymentMethods[position])
        holder.itemView.findViewById<RelativeLayout>(R.id.parent).setOnClickListener {
            clickListener(paymentMethods[position])
        }
    }

    override fun getItemCount(): Int {
        return paymentMethods.size
    }

    fun addList(list: List<PaymentMethod>) {
        paymentMethods.clear()
        paymentMethods.addAll(list)
        notifyItemRangeInserted(0, list.size)
    }

    inner class PaymentMethodsViewholder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val name: TextView by lazy { itemView.findViewById(R.id.txt_payment_option) }

        fun setData(item: PaymentMethod) {
            name.text = getSanitizedText("${item.card?.brand} - ${item.card?.last4}")
        }
    }
}