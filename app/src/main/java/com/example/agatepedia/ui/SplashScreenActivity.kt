package com.example.agatepedia.ui

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import com.example.agatepedia.MainActivity
import com.example.agatepedia.R
import com.example.agatepedia.databinding.ActivitySplashScreenBinding

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app_splash = AnimationUtils.loadAnimation(this, R.anim.app_splash)
        binding.iconSplash.startAnimation(app_splash)


        Handler(Looper.getMainLooper()).postDelayed({

            // Delay and Start Activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 2000) // here we're delaying to startActivity after 2seconds

    }
}