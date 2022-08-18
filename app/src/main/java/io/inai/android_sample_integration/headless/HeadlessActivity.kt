package io.inai.android_sample_integration.headless

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import io.inai.android_sample_integration.R
import kotlinx.android.synthetic.main.activity_headless.*

class HeadlessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_headless)
        //  Remove Title bar
        supportActionBar?.hide()
    }

    fun showProgress() {
        progressBar.visibility = View.VISIBLE
    }

    fun hideProgress() {
        progressBar.visibility = View.INVISIBLE
    }

    fun showProgress() {
        progressBar.visibility = View.VISIBLE
    }

    fun hideProgress() {
        progressBar.visibility = View.INVISIBLE
    }
}