package io.inai.android_sample_integration.drop_in

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.inai.android_sample_integration.R

class DropInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drop_in)
        //  Remove Title bar
        supportActionBar?.hide()
    }
}