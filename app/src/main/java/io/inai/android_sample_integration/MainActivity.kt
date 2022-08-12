package io.inai.android_sample_integration

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import io.inai.android_sample_integration.headless.HeadlessActivity

class MainActivity : AppCompatActivity() {

    private lateinit var headless : Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        headless = findViewById<Button>(R.id.btn_headless)

        headless.setOnClickListener {
            val intent = Intent(this,HeadlessActivity::class.java)
            startActivity(intent)
        }
    }
}