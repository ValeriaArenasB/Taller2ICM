package com.example.taller2icm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.example.taller2icm.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.contacts.setOnClickListener {
            startActivity(Intent(baseContext, ContactsActivity::class.java))
        }
        binding.images.setOnClickListener {
            startActivity(Intent(baseContext, CameraActivity::class.java))
        }
        binding.map.setOnClickListener {
            startActivity(Intent(baseContext, LocationActivity::class.java))
        }


    }
}