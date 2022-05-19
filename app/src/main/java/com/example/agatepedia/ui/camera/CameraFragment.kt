package com.example.agatepedia.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.example.agatepedia.databinding.FragmentCameraBinding
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.agatepedia.R
import com.example.agatepedia.ui.detailagatepedia.DetailAgatepediaActivityArgs
import com.example.agatepedia.utils.createFile
import com.example.agatepedia.utils.uriToFiles
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
                if (lensFacing.equals(CameraSelector.LENS_FACING_BACK)) CameraSelector.LENS_FACING_FRONT
                else CameraSelector.LENS_FACING_BACK

            setupCameraWithPermissionCheck()
        }

        binding.galery.setOnClickListener { view ->
            openGalery(view)
        }

        binding.captureImage.setOnClickListener { view ->
            takePhoto(view)
        }
    }

    override fun onResume() {
        super.onResume()
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

//            Create a new camera selector each time, enforcing lens facing
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

//            Apply declared configs to CameraX using the same lifecycle owner
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
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
        private val TAG = "camera"
    }
}