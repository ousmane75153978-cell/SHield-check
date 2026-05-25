package com.shieldcheck.app

import android.content.Context
import io.github.supabase_community.exceptions.RestException
import io.github.supabase_community.gotrue.GoTrue
import io.github.supabase_community.postgrest.Postgrest
import io.github.supabase_community.realtime.Realtime
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import kotlinx.serialization.json.Json

object SupabaseClient {
    private var instance: SupabaseClientWrapper? = null

    fun getInstance(context: Context): SupabaseClientWrapper {
        if (instance == null) {
            // Récupération des secrets depuis les variables d'environnement ou SharedPreferences
            val supabaseUrl = System.getenv("SUPABASE_URL") ?: getSecretFromPreferences(context, "SUPABASE_URL") ?: ""
            val supabaseAnonKey = System.getenv("SUPABASE_ANON_KEY") ?: getSecretFromPreferences(context, "SUPABASE_ANON_KEY") ?: ""

            instance = SupabaseClientWrapper(
                url = supabaseUrl,
                anonKey = supabaseAnonKey
            )
        }
        return instance!!
    }

    private fun getSecretFromPreferences(context: Context, key: String): String? {
        val prefs = context.getSharedPreferences("supabase_secrets", Context.MODE_PRIVATE)
        return prefs.getString(key, null)
    }
}

class SupabaseClientWrapper(
    val url: String,
    val anonKey: String
) {
    private val httpClient = HttpClient(Android)
    private val json = Json { ignoreUnknownKeys = true }

    // Initialisation du client Supabase avec les endpoints
    val postgrest: Postgrest = Postgrest(
        baseUrl = "$url/rest/v1",
        headers = mapOf(
            "apikey" to anonKey,
            "Authorization" to "Bearer $anonKey"
        ),
        httpClient = httpClient,
        json = json
    )

    val realtime: Realtime = Realtime(
        baseUrl = url.replace("https://", "wss://").replace("http://", "ws://") + "/realtime/v1",
        headers = mapOf(
            "apikey" to anonKey
        ),
        json = json
    )
}
