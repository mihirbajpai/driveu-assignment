package com.example.driveu_mihirbajpai.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.driveu_mihirbajpai.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashScreen : AppCompatActivity() {
    private val SPLASH_DELAY: Long = 3000 // 3 seconds delay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val imageViewLogo: ImageView = findViewById(R.id.imageViewLogo)

        val zoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in)
        imageViewLogo.startAnimation(zoomInAnimation)

        lifecycleScope.launch(Dispatchers.IO) {
            fetchData()
        }
    }

    private suspend fun fetchData() {
        delay(2000)

        navigateToMainActivity()
    }

    private fun navigateToMainActivity() {
        Handler(mainLooper).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1000)
    }
}
