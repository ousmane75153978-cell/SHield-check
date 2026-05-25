# ShieldCheck - Application de Protection d'Appareils

## Description
ShieldCheck est une application Android moderne conçue pour localiser et verrouiller les appareils volés en temps réel. Elle utilise Supabase comme backend pour l'écoute en temps réel de la base de données des objets volés.

## Fonctionnalités
- ✅ Récupération automatique de l'IMEI de l'appareil
- ✅ Écoute en temps réel de la table `objets_voles_reel` via Supabase
- ✅ Verrouillage automatique si l'IMEI est détecté comme volé
- ✅ Activation des droits d'administration au lancement
- ✅ Service de fond persistant même après redémarrage
- ✅ Notifications de statut en temps réel

## Architecture

### Structure du Projet
```
SHield-check/
├── app/
│   ├── src/main/
│   │   ├── java/com/shieldcheck/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── DeviceAdminReceiver.kt
│   │   │   ├── LockService.kt
│   │   │   ├── BootCompletedReceiver.kt
│   │   │   ├── SupabaseClient.kt
│   │   │   └── SharedPreferencesHelper.kt
│   │   ├── res/layout/activity_main.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Permissions Requises
- `android.permission.READ_PHONE_STATE`
- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`
- `android.permission.RECEIVE_BOOT_COMPLETED`
- `android.permission.FOREGROUND_SERVICE`

## Flux de Fonctionnement

1. **Lancement**: MainActivity demande les droits d'administrateur
2. **Service**: LockService démarre automatiquement
3. **Récupération IMEI**: L'appareil récupère son identifiant unique
4. **Monitoring**: Vérification toutes les 10 secondes de Supabase
5. **Détection**: Si IMEI trouvé → verrouillage immédiat
6. **Sécurité**: Si IMEI non trouvé → appareil reste actif

## Dépendances

- Supabase KT 1.4.7
- Ktor Client Android 2.3.4
- AndroidX Core, AppCompat
- Coroutines 1.7.1
- Serialization JSON 1.6.1

## Configuration Supabase

Les clés Supabase doivent être définies:
- `SUPABASE_URL`: URL de votre instance Supabase
- `SUPABASE_ANON_KEY`: Clé anonyme d'accès

## Build et Déploiement

### Générer l'APK
```bash
./gradlew build
```

### Chemins de sortie
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Schéma Supabase Attendu

```sql
CREATE TABLE objets_voles_reel (
  id BIGSERIAL PRIMARY KEY,
  imei TEXT NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT NOW()
);
```

## Installation sur appareil

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Développement

1. Cloner le repository
2. Ouvrir dans Android Studio
3. Configurer les variables d'environnement Supabase
4. Synchroniser Gradle
5. Lancer sur un émulateur ou appareil physique

## Support

Documentation Supabase: https://supabase.com/docs

## Licence

MIT
