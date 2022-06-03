package com.example.agatepedia.ui.detailagatepedia

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.navArgs
import com.example.agatepedia.databinding.ActivityDetailAgatepediaBinding
import com.example.agatepedia.ml.Model
import com.example.agatepedia.utils.Category
import com.example.agatepedia.utils.loadLabel
import com.example.agatepedia.utils.rotateBitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class DetailAgatepediaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailAgatepediaBinding

    private val args: DetailAgatepediaActivityArgs by navArgs()

    //process image resize to 300 x 300
    private val tfImageProcessor by lazy {
        ImageProcessor.Builder()
            .add(ResizeOp(IMG_SIZE_X, IMG_SIZE_Y, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(NORMALIZE_MEAN, NORMALIZE_STD))
            .build()
    }


    private val tfImage = TensorImage(DataType.FLOAT32)


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

        predict(result)

    }

    private fun predict(bitmap: Bitmap) {
        val agateModel = Model.newInstance(this)
        val label = loadLabel(this)


        // converting bitmap into tensor flow image
        val newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        tfImage.load(newBitmap)

        val tensorImage = tfImageProcessor.process(tfImage)

//        process the tensorImage
        val outputs = agateModel.process(tensorImage.tensorBuffer).outputFeature0AsTensorBuffer

        //unify labels with scores into a list
        val probabilistAsCategory = mutableListOf<Category>()

        for (i in 0..outputs.floatArray.size - 1) {
            probabilistAsCategory.add(Category(label[i], outputs.floatArray[i]))
        }


        val outputAgate = probabilistAsCategory.apply { sortByDescending { it.score } }
        binding.tvTitle.text = "${outputAgate[0].labelName} " + "%.0f".format(outputAgate[0].score * 100) + "%"
    }

    companion object {
        //Model input size
        private const val IMG_SIZE_X = 300
        private const val IMG_SIZE_Y = 300

        private const val NORMALIZE_MEAN = 0f
        private const val NORMALIZE_STD = 1f

    }
}