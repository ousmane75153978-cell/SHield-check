package com.shieldcheck.app

import android.content.Context

object SharedPreferencesHelper {
    private const val PREF_NAME = "supabase_config"

    fun saveSupabaseConfig(context: Context, url: String, anonKey: String) {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("SUPABASE_URL", url)
            putString("SUPABASE_ANON_KEY", anonKey)
            apply()
        }
    }

    fun getSupabaseUrl(context: Context): String? {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString("SUPABASE_URL", null)
    }

    fun getSupabaseAnonKey(context: Context): String? {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString("SUPABASE_ANON_KEY", null)
    }
}
