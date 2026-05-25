package com.shieldcheck.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class LockService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private var isDeviceAdmin = false
    private var lastCheckTime = 0L
    private val CHECK_INTERVAL = TimeUnit.SECONDS.toMillis(10) // Vérifier toutes les 10 secondes

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "ShieldCheckChannel"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("LockService", "Service created")
        
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(this, ShieldCheckDeviceAdminReceiver::class.java)
        isDeviceAdmin = devicePolicyManager.isAdminActive(componentName)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        startRealtimeListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LockService", "Service started")
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ShieldCheck Protection",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ShieldCheck")
            .setContentText("Protection en cours...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startRealtimeListener() {
        serviceScope.launch {
            try {
                val context = this@LockService
                val deviceImei = getDeviceImei()
                
                Log.d("LockService", "Device IMEI: $deviceImei")
                Log.d("LockService", "Device Admin active: $isDeviceAdmin")
                
                // Initialiser Supabase
                val supabaseClient = SupabaseClient.getInstance(context)
                Log.d("LockService", "Supabase client initialized")
                
                // Démarrer la boucle de monitoring
                monitorStolenDevices(supabaseClient, deviceImei)
            } catch (e: Exception) {
                Log.e("LockService", "Error starting realtime listener", e)
                // Réessayer après 5 secondes
                delay(5000)
                startRealtimeListener()
            }
        }
    }

    private suspend fun monitorStolenDevices(supabaseClient: SupabaseClientWrapper, deviceImei: String) {
        var consecutiveErrors = 0
        val maxRetries = 5
        
        while (true) {
            try {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastCheckTime >= CHECK_INTERVAL) {
                    checkDeviceStatus(supabaseClient, deviceImei)
                    lastCheckTime = currentTime
                    consecutiveErrors = 0 // Réinitialiser le compteur en cas de succès
                }
                delay(1000) // Vérifier toutes les secondes
            } catch (e: Exception) {
                consecutiveErrors++
                Log.e("LockService", "Error checking device status (attempt $consecutiveErrors/$maxRetries)", e)
                
                if (consecutiveErrors >= maxRetries) {
                    Log.w("LockService", "Max retries reached, restarting listener")
                    consecutiveErrors = 0
                    delay(5000)
                    monitorStolenDevices(supabaseClient, deviceImei)
                    return
                }
                delay(2000)
            }
        }
    }

    private suspend fun checkDeviceStatus(supabaseClient: SupabaseClientWrapper, deviceImei: String) {
        try {
            Log.d("LockService", "Checking device status for IMEI: $deviceImei")
            
            // Récupérer les données de la table objets_voles_reel
            val response = try {
                supabaseClient.postgrest.from("objets_voles_reel")
                    .select()
                    .execute()
            } catch (e: Exception) {
                Log.e("LockService", "Failed to query Supabase", e)
                throw e
            }
            
            val responseText = response.body?.toString() ?: ""
            Log.d("LockService", "Supabase response received, length: ${responseText.length}")
            
            // Vérifier si l'IMEI de l'appareil est présent dans la table
            val isStolen = responseText.contains(deviceImei, ignoreCase = true) ||
                          responseText.contains(getDeviceImei(), ignoreCase = true)
            
            if (isStolen) {
                Log.w("LockService", "⚠️  ALERT! Device IMEI found in stolen list - LOCKING DEVICE NOW")
                lockNow()
            } else {
                Log.d("LockService", "✓ Device is safe - IMEI not in stolen list")
            }
        } catch (e: Exception) {
            Log.e("LockService", "Error checking device status", e)
            throw e
        }
    }

    private fun lockNow() {
        if (!isDeviceAdmin) {
            Log.w("LockService", "Cannot lock device - Device admin rights not granted")
            return
        }
        
        try {
            // Verrouiller l'écran
            devicePolicyManager.lockNow()
            Log.d("LockService", "✓ Device successfully locked")
        } catch (e: Exception) {
            Log.e("LockService", "Error locking device", e)
        }
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
        serviceScope.launch {
            delay(1000) // Attendre avant de redémarrer
            startRealtimeListener()
        }
    }
}
