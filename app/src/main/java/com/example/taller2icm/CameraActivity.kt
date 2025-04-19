package com.example.taller2icm

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.taller2icm.databinding.ActivityCameraBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

const val PERMISSION_GALLERY = 101
const val PERMISSION_CAMERA = 102

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var photoUri: Uri
    private lateinit var currentPhotoFile: File

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(),
        ActivityResultCallback { it?.let { loadImage(it) } }
    )

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
        ActivityResultCallback { success ->
            if (success) {
                saveImageToGallery(currentPhotoFile)
                loadImage(photoUri)
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGallery.setOnClickListener {
            requestGalleryPermission()
        }

        binding.btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    PERMISSION_CAMERA
                )
            }
        }
    }

    private fun launchCamera() {
        currentPhotoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            baseContext,
            baseContext.packageName + ".fileprovider",
            currentPhotoFile
        )
        cameraLauncher.launch(photoUri)
    }

    private fun createImageFile(): File {
        val fileName = "IMG_${System.currentTimeMillis()}"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDir)
    }

    private fun saveImageToGallery(file: File) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraApp")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            contentResolver.openOutputStream(it).use { output ->
                file.inputStream().copyTo(output!!)
            }
        }
    }

    private fun requestGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED) {
                galleryLauncher.launch("image/*")
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    PERMISSION_GALLERY
                )
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED) {
                galleryLauncher.launch("image/*")
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_GALLERY
                )
            }
        }
    }

    private fun loadImage(uri: Uri) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        val width = options.outWidth
        val height = options.outHeight
        Log.d("CameraActivity", "Image resolution: ${width}x${height}")

        // Mostrar la imagen redimensionada (ajustada por el ImageView)
        val imageStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(imageStream)
        binding.image.setImageBitmap(bitmap)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                PERMISSION_GALLERY -> galleryLauncher.launch("image/*")
                PERMISSION_CAMERA -> launchCamera()
            }
        } else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }
}
