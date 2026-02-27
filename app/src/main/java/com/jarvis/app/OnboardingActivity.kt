package com.jarvis.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class OnboardingActivity : AppCompatActivity() {

    private val microphonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val contactsSmsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        findViewById<Button>(R.id.btnMicrophone).setOnClickListener {
            requestMicrophonePermission()
        }

        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.btnNotifications).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.btnContactsSms).setOnClickListener {
            requestContactsSmsPermissions()
        }

        startService(Intent(this, com.jarvis.app.runtime.AssistantForegroundService::class.java))
    }

    private fun requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun requestContactsSmsPermissions() {
        val permissions = mutableListOf(Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        contactsSmsPermissionLauncher.launch(permissions.toTypedArray())
    }
}
