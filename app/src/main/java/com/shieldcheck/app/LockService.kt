package com.shieldcheck.app

import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class LockService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private var isDeviceAdmin = false

    override fun onCreate() {
        super.onCreate()
        Log.d("LockService", "Service created")
        
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(this, ShieldCheckDeviceAdminReceiver::class.java)
        isDeviceAdmin = devicePolicyManager.isAdminActive(componentName)
        
        startRealtimeListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LockService", "Service started")
        return START_STICKY
    }

    private fun startRealtimeListener() {
        serviceScope.launch {
            try {
                val supabaseClient = SupabaseClient.getInstance(this@LockService)
                val deviceImei = getDeviceImei()
                
                Log.d("LockService", "Device IMEI: $deviceImei")
                
                // Écouter les changements en temps réel sur la table objets_voles_reel
                listenToStolenDevices(supabaseClient, deviceImei)
            } catch (e: Exception) {
                Log.e("LockService", "Error starting realtime listener", e)
                // Réessayer après 5 secondes
                delay(5000)
                startRealtimeListener()
            }
        }
    }

    private suspend fun listenToStolenDevices(supabaseClient: SupabaseClientWrapper, deviceImei: String) {
        try {
            // Vérifier d'abord l'état actuel
            checkCurrentDeviceStatus(supabaseClient, deviceImei)
            
            // Mettre en place l'écoute temps réel
            // Note: Supabase Realtime nécessite une configuration spécifique
            // Pour maintenant, nous utilisons un polling toutes les 10 secondes
            while (true) {
                delay(10000) // Vérifier toutes les 10 secondes
                checkCurrentDeviceStatus(supabaseClient, deviceImei)
            }
        } catch (e: Exception) {
            Log.e("LockService", "Error in realtime listener", e)
        }
    }

    private suspend fun checkCurrentDeviceStatus(supabaseClient: SupabaseClientWrapper, deviceImei: String) {
        try {
            // Récupérer les données de la table objets_voles_reel
            val response = supabaseClient.postgrest.from("objets_voles_reel")
                .select()
                .execute()
            
            val body = response.body as? String ?: return
            Log.d("LockService", "Response: $body")
            
            // Vérifier si l'IMEI de l'appareil est présent dans la table
            if (body.contains(deviceImei, ignoreCase = true)) {
                Log.w("LockService", "Device IMEI found in stolen list - Locking device")
                lockNow()
            } else {
                Log.d("LockService", "Device IMEI not in stolen list - Device is safe")
                // Réinitialiser les accès si nécessaire
                unlockDevice()
            }
        } catch (e: Exception) {
            Log.e("LockService", "Error checking device status", e)
        }
    }

    private fun lockNow() {
        if (!isDeviceAdmin) {
            Log.w("LockService", "Device admin rights not granted")
            return
        }
        
        try {
            // Verrouiller l'écran
            devicePolicyManager.lockNow()
            Log.d("LockService", "Device locked")
        } catch (e: Exception) {
            Log.e("LockService", "Error locking device", e)
        }
    }

    private fun unlockDevice() {
        // Note: Android n'autorise pas les apps à déverrouiller directement pour des raisons de sécurité
        Log.d("LockService", "Device unlock reset - User can unlock via PIN/Pattern/Password")
    }

    private fun getDeviceImei(): String {
        return try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telephonyManager.imei
            } else {
                telephonyManager.deviceId
            }
        } catch (e: Exception) {
            Log.e("LockService", "Error getting device IMEI", e)
            "UNKNOWN"
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LockService", "Service destroyed")
    }
}
