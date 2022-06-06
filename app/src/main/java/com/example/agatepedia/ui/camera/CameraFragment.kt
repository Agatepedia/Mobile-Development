package com.example.agatepedia.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.example.agatepedia.databinding.FragmentCameraBinding
import java.util.concurrent.Executors
import com.example.agatepedia.R
import com.example.agatepedia.ml.Model
import com.example.agatepedia.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import permissions.dispatcher.*

@RuntimePermissions
class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private val executor = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private lateinit var viewGalery: View
    private lateinit var bitmapBuffer: Bitmap
    private var imageRotationDegress: Int = 0
    private var pauseImage = false
    private lateinit var camera: Camera
    private var stateFlash = false
    private lateinit var safeContext: Context

    //process image resize to 300 x 300
    private val tfImageProcessor by lazy {
        ImageProcessor.Builder()
            .add(ResizeOp(IMG_SIZE_X, IMG_SIZE_Y, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(NORMALIZE_MEAN, NORMALIZE_STD))
            .add(Rot90Op(-imageRotationDegress / 90))
            .build()
    }

    private val tfImage = TensorImage(DataType.FLOAT32)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.switchCamera.setOnClickListener {
            lensFacing =
                if (lensFacing.equals(CameraSelector.LENS_FACING_BACK)) {
                    binding.flash.visibility = View.GONE

//                    turn off flash
                    binding.flash.setImageDrawable(resources.getDrawable(R.drawable.ic_flash_on))
                    stateFlash = false

                    CameraSelector.LENS_FACING_FRONT
                } else {
                    binding.flash.visibility = View.VISIBLE
                    CameraSelector.LENS_FACING_BACK
                }

            setupCameraWithPermissionCheck()
        }

        binding.galery.setOnClickListener { view ->
            openGalery(view)
        }

        binding.captureImage.setOnClickListener { view ->
            takePhoto(view)
        }

        binding.flash.setOnClickListener() { flashCamera() }
    }

    override fun onResume() {
        super.onResume()

//  hide image preview
        if (pauseImage) {
            pauseImage = false
            binding.imagePriview.visibility = View.GONE
        }


        setupCameraWithPermissionCheck()
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

//            set up view finder use case to display camera view
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(binding.viewFinder.display.rotation)
                .build()
                .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

            //setup the image capture use case
            imageCapture = ImageCapture.Builder().build()

//            Set up the image analysis use case which will process frames in real time
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(binding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
                if (!::bitmapBuffer.isInitialized) {
                    bitmapBuffer =
                        Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
                    imageRotationDegress = image.imageInfo.rotationDegrees
                }

                image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
                if (_binding != null) {
                    predict(bitmapBuffer)
                }
            })

//            Create a new camera selector each time, enforcing lens facing
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

//            Apply declared configs to CameraX using the same lifecycle owner
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    requireActivity() as LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera Fail", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun predict(bitmap: Bitmap) {
        val agateModel = Model.newInstance(safeContext)
        val label = loadLabel(safeContext)


        // converting bitmap into tensor flow image
        val newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        tfImage.load(newBitmap)
        //resize 300 x 300
        val tensorImage = tfImageProcessor.process(tfImage)

//        process the tensorImage
        val outputs = agateModel.process(tensorImage.tensorBuffer).outputFeature0AsTensorBuffer

        val probabilistAsCategory = mutableListOf<Category>()

        for (i in 0..outputs.floatArray.size - 1) {
            probabilistAsCategory.add(Category(label[i], outputs.floatArray[i]))
        }

        val outputAgate = probabilistAsCategory.apply { sortByDescending { it.score } }
        if (_binding != null) {
            lifecycleScope.launch(Dispatchers.Main) {
                binding.tvPredict1.text =
                    "${outputAgate[0].labelName} " + "%.0f".format(outputAgate[0].score * 100) + "%"
                binding.tvPredict2.text =
                    "${outputAgate[1].labelName} " + "%.0f".format(outputAgate[1].score * 100) + "%"
                binding.tvPredict3.text =
                    "${outputAgate[2].labelName} " + "%.0f".format(outputAgate[2].score * 100) + "%"
            }
        }

    }

    private fun flashCamera() {
        val changeState = !stateFlash
        if (!stateFlash) {
            binding.flash.setImageDrawable(resources.getDrawable(R.drawable.ic_flash_off))
            camera.cameraControl.enableTorch(true)
        } else {
            binding.flash.setImageDrawable(resources.getDrawable(R.drawable.ic_flash_on))
            camera.cameraControl.enableTorch(false)
        }
        stateFlash = changeState

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    fun showRationaleForCamera(request: PermissionRequest) {
        showDialog(R.string.message_permission_camera, request)
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    fun permissionCameraDenied() {
        Toast.makeText(
            requireContext(),
            getString(R.string.denied_permission_camera),
            Toast.LENGTH_SHORT
        ).show()
        requireActivity().onBackPressed()
    }

    private fun showDialog(@StringRes messageResId: Int, request: PermissionRequest) {
        AlertDialog.Builder(requireContext())
            .setPositiveButton(R.string.button_allow) { _, _ -> request.proceed() }
            .setNegativeButton(R.string.button_deny) { _, _ -> request.cancel() }
            .setCancelable(false)
            .setMessage(messageResId)
            .show()
    }


    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        delegate the permission handling to generated function
        onRequestPermissionsResult(requestCode, grantResults)
    }

    @SuppressLint("IntentReset")
    private fun openGalery(view: View) {
        viewGalery = view
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        val mimeTypes = arrayOf("image/jpg", "image/png", "image/jpeg")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        launcherGalery.launch(intent)
    }

    private val launcherGalery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result?.resultCode == Activity.RESULT_OK) {
            val selectedImage: Uri = result.data?.data as Uri
            val myPhoto = uriToFiles(selectedImage, requireContext())
            val toDetailAgateActivity =
                CameraFragmentDirections.actionNavigationDashboardToDetailAgatepediaActivity(myPhoto)
            toDetailAgateActivity.isCamera = false
            viewGalery.findNavController().navigate(toDetailAgateActivity)
        }
    }

    private fun takePhoto(view: View) {
        binding.proggressBar.visibility = View.VISIBLE
        binding.captureImage.isEnabled = false

        val imageCapture = imageCapture ?: return
        val photoFile = activity?.let { createFile(it.application) }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile!!).build()

//        pause image while pressing capture image
        if (!pauseImage) {
            pauseImage = true

            val isFrontFacing = lensFacing.equals(CameraSelector.LENS_FACING_FRONT)

            val matrix = Matrix().apply {
                postRotate(imageRotationDegress.toFloat())
                if (isFrontFacing) {
                    postRotate(-180f)
                    postScale(-1f, 1f)
                }
            }


            val freezingImage = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true
            )

            binding.imagePriview.setImageBitmap(freezingImage)
            binding.imagePriview.visibility = View.VISIBLE
        }

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val toDetailAgateActivity =
                        CameraFragmentDirections.actionNavigationDashboardToDetailAgatepediaActivity(
                            photoFile
                        )
                    toDetailAgateActivity.isBackCamera =
                        lensFacing == CameraSelector.LENS_FACING_BACK
                    toDetailAgateActivity.isCamera = true
                    view.findNavController().navigate(toDetailAgateActivity)
                    binding.proggressBar.visibility = View.GONE
                    binding.captureImage.isEnabled = true

//                    turn off flash camera
                    binding.flash.setImageDrawable(resources.getDrawable(R.drawable.ic_flash_on))
                    stateFlash = false
                    camera.cameraControl.enableTorch(false)
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.failed_save_photo),
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.proggressBar.visibility = View.GONE
                    binding.captureImage.isEnabled = true
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val TAG = CameraFragment::class.simpleName

        //Model input size
        private const val IMG_SIZE_X = 300
        private const val IMG_SIZE_Y = 300

        private const val NORMALIZE_MEAN = 0f
        private const val NORMALIZE_STD = 1f
    }
}