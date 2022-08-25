package io.inai.android_sample_integration

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import io.inai.android_sample_integration.drop_in.DropInActivity
import io.inai.android_sample_integration.headless.HeadlessActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //  Remove Title bar
        supportActionBar?.title = "Android Sample Integration"

        btn_headless.setOnClickListener {
            val intent = Intent(this,HeadlessActivity::class.java)
            startActivity(intent)
        }

        btn_dropin.setOnClickListener {
            val intent = Intent(this,DropInActivity::class.java)
            startActivity(intent)
        }
    }
}