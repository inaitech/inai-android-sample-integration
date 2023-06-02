package io.inai.android_sample_integration.headless.make_payment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.helpers.Constants
import io.inai.android_sample_integration.helpers.getSanitizedText
import org.json.JSONArray
import org.json.JSONObject

class PaymentOptionsAdapterNew() :
    RecyclerView.Adapter<PaymentOptionsAdapterNew.PaymentOptionsViewholder>() {

    private val paymentOptions: MutableList<PaymentMethodOption> = mutableListOf()
    private val walletPaymentOptions: MutableList<PaymentMethodOption> = mutableListOf()

    var clickListener: (PaymentMethodOption) -> Unit = { }
    var payBtnClickListener: (JSONObject, PaymentMethodOption) -> Unit = { jsonObject: JSONObject, paymentMethodOption: PaymentMethodOption -> }

    private lateinit var formBuilder: FormBuilder
    private var upiIntentPaymentDetails  = JSONObject()
    private lateinit var upiPaymentMethodOption: PaymentMethodOption

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentOptionsViewholder {
        formBuilder = FormBuilder(parent.context)
        return PaymentOptionsViewholder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_payment_options_new, parent, false)
        )
    }

    override fun onBindViewHolder(holder: PaymentOptionsViewholder, position: Int) {
        holder.setData(paymentOptions[position])
        holder.itemView.findViewById<CardView>(R.id.card_view).setOnClickListener {
            clickListener(it.tag as PaymentMethodOption)
        }
        holder.itemView.findViewById<LinearLayout>(R.id.ll_upi_collect).setOnClickListener {
            clickListener(it.tag as PaymentMethodOption)
        }
        holder.itemView.findViewById<Button>(R.id.btn_pay_upi_intent).setOnClickListener {
            payBtnClickListener(upiIntentPaymentDetails,upiPaymentMethodOption)
        }
    }


    override fun getItemCount(): Int {
        return paymentOptions.size
    }

    fun addList(list: List<PaymentMethodOption>) {
        paymentOptions.clear()
        walletPaymentOptions.clear()
        paymentOptions.addAll(list)
        notifyItemRangeRemoved(0, list.size)
    }

    inner class PaymentOptionsViewholder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView by lazy { itemView.findViewById(R.id.txt_payment_option) }
        private val cardView: CardView by lazy { itemView.findViewById(R.id.card_view) }
        private val paymentMethodLayout: LinearLayout by lazy { itemView.findViewById(R.id.ll_paymentMode) }

        //UPI related view
        private val upiCardView: CardView by lazy { itemView.findViewById(R.id.upi_card_view) }
        private val upiIntentLayout: LinearLayout by lazy { itemView.findViewById(R.id.ll_upi_intent) }
        private val upiCollectLayout: LinearLayout by lazy { itemView.findViewById(R.id.ll_upi_collect) }
        private val upiIntentPayBtn: Button by lazy { itemView.findViewById(R.id.btn_pay_upi_intent) }

        fun setData(item: PaymentMethodOption) {
            if(item.category == Constants.CATEGORY_UPI){
                cardView.visibility = View.GONE
                upiCardView.visibility = View.VISIBLE
                name.text = getSanitizedText(item.category ?: "")
                upiPaymentMethodOption = item
                val upiIntentMode = item.modes?.filter {
                    it.code == Constants.MODE_CODE_UPI_INTENT
                }

                upiIntentLayout.tag = item
                upiIntentPayBtn.tag = upiIntentMode
                if(upiIntentMode!=null && upiIntentMode.isNotEmpty()){
                    upiIntentLayout.addView(formBuilder.createRadioButtonGroup(upiIntentMode!![0].formFields[0]))
                    val rg = upiIntentLayout.findViewWithTag<RadioGroup>(upiIntentMode!![0].formFields[0])
                    rg.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { radioGroup, i ->
                        val radioButton = upiIntentLayout.findViewById<RadioButton>(radioGroup.checkedRadioButtonId)
                        if (radioButton != null)
                            updateUpiIntentPaymentDetails(radioButton.tag.toString(),upiIntentPayBtn.tag as List<Mode>)
                        upiIntentPayBtn.visibility = View.VISIBLE
                    })
                }

                upiCollectLayout.tag = item
                upiCollectLayout.addView(formBuilder.createDividerLine())
                upiCollectLayout.addView(formBuilder.createUPICollectLayout())
            }else{
                upiCardView.visibility = View.GONE
                cardView.tag = item
                name.text = getSanitizedText(item.category ?: "")
                paymentMethodLayout.addView(formBuilder.createLabelWithImage(item))
            }
        }
    }

    private fun updateUpiIntentPaymentDetails(selectedValue: String, modes: List<Mode>) {
        val mode = modes[0].code
        val fieldName = modes[0].formFields[0].name
        val fieldsArray = JSONArray()
        val paymentField = getPaymentField(fieldName!!,selectedValue)
        fieldsArray.put(paymentField)
        upiIntentPaymentDetails.put("mode", mode)
        upiIntentPaymentDetails.put("fields", fieldsArray)
    }

    private fun getPaymentField(name: String, value: Any): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("name", name)
        jsonObject.put("value", value)
        return jsonObject
    }
}