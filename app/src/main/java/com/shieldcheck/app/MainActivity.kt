package com.shieldcheck.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private val DEVICE_ADMIN_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "App started")

        // Initialiser le gestionnaire de politique d'appareil
        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(this, ShieldCheckDeviceAdminReceiver::class.java)

        // Éléments UI
        val statusText: TextView = findViewById(R.id.statusText)
        val adminButton: Button = findViewById(R.id.adminButton)
        val startServiceButton: Button = findViewById(R.id.startServiceButton)

        // Vérifier l'état du droit administrateur
        updateAdminStatus(statusText)

        // Demander les droits d'administrateur
        adminButton.setOnClickListener {
            requestAdminPermissions()
        }

        // Démarrer le service
        startServiceButton.setOnClickListener {
            startLockService()
        }

        // Démarrer automatiquement le service au lancement
        startLockService()
    }

    private fun updateAdminStatus(statusText: TextView) {
        val isAdmin = devicePolicyManager.isAdminActive(componentName)
        statusText.text = if (isAdmin) {
            "✓ Device Admin activé"
        } else {
            "✗ Device Admin non activé"
        }
        Log.d("MainActivity", "Admin status: $isAdmin")
    }

    private fun requestAdminPermissions() {
        if (!devicePolicyManager.isAdminActive(componentName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "ShieldCheck nécessite les droits d'administrateur pour protéger votre appareil"
            )
            startActivityForResult(intent, DEVICE_ADMIN_REQUEST)
        }
    }

    private fun startLockService() {
        Log.d("MainActivity", "Starting LockService")
        val serviceIntent = Intent(this, LockService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DEVICE_ADMIN_REQUEST) {
            val statusText: TextView = findViewById(R.id.statusText)
            updateAdminStatus(statusText)
            if (resultCode == RESULT_OK) {
                Log.d("MainActivity", "Admin rights granted")
            }
        }
    }
}
