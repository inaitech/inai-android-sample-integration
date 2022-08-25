package io.inai.android_sample_integration.drop_in

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import androidx.navigation.findNavController
import io.inai.android_sample_integration.BuildConfig
import io.inai.android_sample_integration.Config
import io.inai.android_sample_integration.MainActivity.Companion.ARG_PAYMENT_OPERATION
import io.inai.android_sample_integration.PaymentOperation
import io.inai.android_sample_integration.R
import io.inai.android_sample_integration.headless.HeadlessFragment
import io.inai.android_sample_integration.helpers.NetworkRequestHandler
import io.inai.android_sample_integration.helpers.json
import io.inai.android_sample_integration.helpers.showAlert
import io.inai.android_sdk.*
import kotlinx.android.synthetic.main.activity_drop_in.*
import kotlinx.coroutines.delay
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class DropInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drop_in)
        //  Remove Title bar
        supportActionBar?.hide()
        Handler(Looper.getMainLooper()).postDelayed({
            findNavController(R.id.dropin_host_fragment).setGraph(
                R.navigation.dropin_flow_graph,
                Bundle().apply { putSerializable(ARG_PAYMENT_OPERATION, PaymentOperation.PresentCheckout) }
            )
        }, 500)
    }
}