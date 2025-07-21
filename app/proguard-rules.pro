
#-------------------------------------------------------------------------------------
# Regole Generali per Android (mantenere i componenti base dell'app)
#-------------------------------------------------------------------------------------
-keep public class com.example.securenotesapp.activities.** { *; }
-keep public class com.example.securenotesapp.fragments.** { *; }
-keep public class com.example.securenotesapp.utils.** { *; } # Le classi utility sono spesso chiamate in modo riflessivo o esterno.
-keep public class com.example.securenotesapp.model.** { *; } # Le classi modello devono mantenere i loro nomi e membri per Room/Gson.
-keep public class com.example.securenotesapp.database.** { *; } # Database Room e TypeConverters.
-keep public class com.example.securenotesapp.dao.** { *; } # Interfacce DAO di Room.
-keep public class com.example.securenotesapp.viewmodel.** { *; } # ViewModel classes.
-keep public class com.example.securenotesapp.repository.** { *; } # Repository classes.

# Mantieni i componenti principali di Android (Activity, Application, Service, ecc.)
# Il sistema operativo li cerca per nome e non devono essere offuscati.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class * extends android.webkit.WebView
-keep public class * extends androidx.fragment.app.Fragment


# Mantiene i costruttori predefiniti per le classi che vengono istanziate dinamicamente.
-keepclassmembers class * {
    public <init>(android.content.Context);
}

# Mantiene le classi che implementano Parcelable
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Mantiene le classi Enum
-keep enum ** { *; }

# Mantiene i metodi nativi (JNI)
-keepclassmembers class * {
    native <methods>;
}

#-------------------------------------------------------------------------------------
# Regole per Librerie AndroidX specifiche
#-------------------------------------------------------------------------------------

# androidx.biometric
-keep class androidx.biometric.** { *; }
-keep class androidx.biometric.BiometricPrompt$PromptInfo
-keep class androidx.biometric.BiometricPrompt$AuthenticationCallback
-keep class androidx.biometric.BiometricManager$Authenticators

# androidx.security.crypto (EncryptedSharedPreferences, EncryptedFile)
-keep class androidx.security.crypto.** { *; }
-keep class androidx.security.crypto.MasterKeys
-keep class androidx.security.crypto.EncryptedSharedPreferences
-keep class androidx.security.crypto.EncryptedFile

# Room Database (androidx.room)
# Necessario per Entità, DAO, Database e TypeConverters.
-keepnames class * extends androidx.room.RoomDatabase
-keepclassmembers class ** {
    @androidx.room.Entity <fields>;
    @androidx.room.Entity <methods>;
    @androidx.room.Dao <fields>;
    @androidx.room.Dao <methods>;
    @androidx.room.Database <fields>;
    @androidx.room.Database <methods>;
    @androidx.room.Embedded <fields>;
    @androidx.room.Relation <fields>;
    @androidx.room.Ignore <fields>;
    @androidx.room.Ignore <methods>;
}
-keep public class * extends androidx.room.RoomDatabase
-keep public class * implements androidx.room.IMultiInstanceInvalidationService
-keep public class androidx.room.util.** { *; }
-keep public class androidx.room.IMultiInstanceInvalidationService
-keep class androidx.room.DatabaseConfiguration
-keep class androidx.room.InvalidationTracker
-keep class androidx.room.RoomDatabase$Callback
-keep class androidx.room.RoomDatabase$JournalMode
-keep class androidx.room.RoomWarnings

# SQLCipher integration (net.zetetic:android-database-sqlcipher e androidx.sqlite)
# Molto importante per non rompere la crittografia del database.
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keep class net.sqlcipher.database.SQLiteDatabase
-keep class net.sqlcipher.database.SQLiteOpenHelper
-keep class net.sqlcipher.database.SQLiteStatement
-keep class net.sqlcipher.database.SQLiteQuery
-keep class net.sqlcipher.database.SQLiteCursor
-keep class net.sqlcipher.database.SQLiteDatabaseHook
-keep class net.sqlcipher.database.SQLiteDebug
-keep class net.sqlcipher.database.SQLiteDirectCursorDriver

# Potrebbe essere necessaria per alcune versioni di SQLCipher o Room
-keep class org.greenrobot.eventbus.** { *; }

# WorkManager (androidx.work)
-keep class androidx.work.** { *; }
-keep class androidx.work.impl.workers.** { *; }
-keep class androidx.work.impl.background.systemjob.** { *; }
-keep class androidx.work.impl.model.** { *; }
-keep class androidx.work.Worker
-keep class androidx.work.ListenableWorker
-keep class androidx.work.Configuration
-keep class androidx.work.ExistingWorkPolicy
-keep class androidx.work.NetworkType
-keep class androidx.work.PeriodicWorkRequest
-keep class androidx.work.WorkInfo
-keep class androidx.work.WorkManager
-keep class androidx.work.OneTimeWorkRequest
-keep class androidx.work.ArrayCreatingInputMerger
-keep class androidx.work.OverwritingInputMerger

# Gson (com.google.code.gson)
# Gson si basa sulla riflessione, quindi ha bisogno di regole per le classi che serializza/deserializza.
# In particolare, le tue classi `model` (Note, FileItem) devono essere mantenute.
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { <init>(); }
-keep class * implements com.google.gson.JsonSerializer { <init>(); }
-keep class * implements com.google.gson.JsonDeserializer { <init>(); }


-keepclassmembers class * {
    void *(android.view.View);
}


-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}

-keep class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    private void readObjectNoData();
}


-dontwarn **.test.**
-dontwarn android.support.test.**


-dontwarn okio.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
