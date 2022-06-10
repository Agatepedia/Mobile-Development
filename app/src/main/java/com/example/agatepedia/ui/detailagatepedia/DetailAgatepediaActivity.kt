package com.example.agatepedia.ui.detailagatepedia

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navArgs
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.agatepedia.R
import com.example.agatepedia.data.Result
import com.example.agatepedia.data.local.entity.AgateEntity
import com.example.agatepedia.databinding.ActivityDetailAgatepediaBinding
import com.example.agatepedia.ml.Model
import com.example.agatepedia.ui.ViewModelFactory
import com.example.agatepedia.ui.home.HomeViewModel
import com.example.agatepedia.utils.Category
import com.example.agatepedia.utils.loadLabel
import com.example.agatepedia.utils.rotateBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class DetailAgatepediaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailAgatepediaBinding

    private val args: DetailAgatepediaActivityArgs by navArgs()

    private var stateFromHome = false
    private lateinit var agateName: String
    private var bookmarkFunctional = false
    private lateinit var agateLocalData: AgateEntity

    private val viewModel: DetailViewModel by viewModels {
        ViewModelFactory.getInstance(this)
    }

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

        stateFromHome = args.isHome
        if (!stateFromHome) {
            showResult()
        } else {
            binding.tvTitle.text = args.nameAgate
            agateName = args.nameAgate.toString()
            searchDataAgate(agateName)
            getBookmark(viewModel, agateName)
        }


        binding.refreshLayout.setOnRefreshListener(object : SwipeRefreshLayout.OnRefreshListener {
            override fun onRefresh() {
                if (!stateFromHome) {
                    showResult()
                } else {
                    searchDataAgate(agateName)
                }

                binding.refreshLayout.isRefreshing = false
            }
        })

        binding.bookmark.setOnClickListener { saveOrDeleteBookmark() }


    }

    private fun showResult() {
        val myPhoto = args.photo
        val isBackCamera = args.isBackCamera
        val isCamera = args.isCamera
        val result: Bitmap

        if (isCamera) result =
            rotateBitmap(BitmapFactory.decodeFile(myPhoto?.path), isBackCamera) else
            result = BitmapFactory.decodeFile(myPhoto?.path)

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
        binding.tvTitle.text =
            "${outputAgate[0].labelName} " + "%.0f".format(outputAgate[0].score * 100) + "%"
        searchDataAgate(outputAgate[0].labelName)
        getBookmark(viewModel, outputAgate[0].labelName)
    }

    fun searchDataAgate(agateName: String) {
        viewModel.searchAgateData(agateName).observe(this) { result ->
            if (result != null) {
                when (result) {
                    is Result.Loading -> {
                        binding.proggressBar.visibility = View.VISIBLE
                    }
                    is Result.Success -> {
                        binding.proggressBar.visibility = View.GONE
                        val agateData = result.data
                        binding.tvDescription.text = agateData[0].penjelasan
                        binding.tvPrice.text = agateData[0].harga
                        if (stateFromHome) {
                            Glide.with(this)
                                .load(agateData[0].gambar)
                                .into(binding.agateImage)
                        }
                        bookmarkFunctional = true

                        agateLocalData =
                            AgateEntity(
                                agateData[0].jenis.toString(),
                                agateData[0].harga.toString(),
                                agateData[0].gambar.toString()
                            )
                    }
                    is Result.Error -> {
                        binding.proggressBar.visibility = View.GONE
                        bookmarkFunctional = false

                        Toast.makeText(
                            this@DetailAgatepediaActivity,
                            getString(R.string.error_request),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun getBookmark(viewModel: DetailViewModel, type: String) {
        lifecycleScope.launch(Dispatchers.Default) {
            val stateBookmarked = viewModel.getStateBookmark(type)

            if (stateBookmarked) {
                withContext(Dispatchers.Main) {
                    binding.bookmark.setImageDrawable(getDrawable(R.drawable.ic_bookmark))
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.bookmark.setImageDrawable(getDrawable(R.drawable.ic_bookmark_border))
                }
            }


        }
    }

    private fun saveOrDeleteBookmark() {
        if (bookmarkFunctional) {
            lifecycleScope.launch(Dispatchers.Default) {
                if (viewModel.getStateBookmark(agateLocalData.type)) {
                    viewModel.deleteBookmark(agateLocalData.type)
                    withContext(Dispatchers.Main) {
                        binding.bookmark.setImageDrawable(getDrawable(R.drawable.ic_bookmark_border))
                        Toast.makeText(
                            this@DetailAgatepediaActivity,
                            getString(R.string.erase_bookmark),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    viewModel.insertBookmark(agateLocalData)
                    withContext(Dispatchers.Main) {
                        binding.bookmark.setImageDrawable(getDrawable(R.drawable.ic_bookmark))
                        Toast.makeText(
                            this@DetailAgatepediaActivity,
                            getString(R.string.add_bookmark),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

        }
    }

    companion object {
        //Model input size
        private const val IMG_SIZE_X = 300
        private const val IMG_SIZE_Y = 300

        private const val NORMALIZE_MEAN = 0f
        private const val NORMALIZE_STD = 1f

    }
}