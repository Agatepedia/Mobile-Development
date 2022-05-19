package com.example.agatepedia.ui.detailagatepedia

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.navigation.navArgs
import com.example.agatepedia.R
import com.example.agatepedia.databinding.ActivityDetailAgatepediaBinding
import com.example.agatepedia.utils.rotateBitmap

class DetailAgatepediaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailAgatepediaBinding

    private val args: DetailAgatepediaActivityArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailAgatepediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showResult()
    }

    private fun showResult() {
        val myPhoto = args.photo
        val isBackCamera = args.isBackCamera
        val isCamera = args.isCamera
        val result: Bitmap

        if (isCamera) result =
            rotateBitmap(BitmapFactory.decodeFile(myPhoto.path), isBackCamera) else
            result = BitmapFactory.decodeFile(myPhoto.path)

        binding.agateImage.setImageBitmap(result)

    }
}