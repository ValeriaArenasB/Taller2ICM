package com.example.taller2icm

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller2icm.databinding.ActivityContactsBinding

const val PERMISSION_READ_CONTACTS = 100

class ContactsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactsBinding


    val projection = arrayOf(ContactsContract.Profile._ID, ContactsContract.Profile.DISPLAY_NAME)
    lateinit var adapter : ContactsAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = ContactsAdapter(this,null,8)
        binding.listContacts.adapter = adapter
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED -> {
                updateUI()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS) -> {
                pedirPermiso(this, Manifest.permission.READ_CONTACTS, "", PERMISSION_READ_CONTACTS)
            }
            else -> {
                pedirPermiso(this, Manifest.permission.READ_CONTACTS, "", PERMISSION_READ_CONTACTS)
            }
        }
    }

    private fun pedirPermiso(context: Activity, permiso: String, justificacion: String, idCode: Int) {
        if (ContextCompat.checkSelfPermission(context, permiso) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, arrayOf(permiso), idCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_READ_CONTACTS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateUI()
                    Toast.makeText(this, "Permiso de contactos concedido", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permiso de contactos denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun updateUI(){
            val cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,projection,null,null,null)
            adapter.changeCursor(cursor)
    }
}