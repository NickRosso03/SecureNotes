package com.example.securenotesapp;

import android.app.Application;
import net.sqlcipher.database.SQLiteDatabase;
import android.util.Log;

/**
 * {@code SecureNotesApplication} è la classe principale dell'applicazione che estende {@link android.app.Application}.
 * Viene utilizzata per inizializzare componenti globali come SQLCipher e {@link FileManager}
 * non appena l'applicazione viene avviata. Gestisce anche la pulizia delle risorse alla terminazione dell'app.
 */
public class SecureNotesApplication extends Application {

    private static final String TAG = "SecureNotesApplication"; //  TAG per i log
    private FileManager fileManager; // Dichiarazione dell'istanza di FileManager, accessibile globalmente.

    /**
     * Chiamato quando l'applicazione viene creata.
     * Questo è il primo punto di ingresso per il codice dell'applicazione dopo il lancio.
     * Inizializza SQLCipher e il FileManager.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Inizializza SQLCipher all'avvio dell'applicazione
        // Questa chiamata è fondamentale per abilitare le operazioni sul database cifrato.
        SQLiteDatabase.loadLibs(this);
        Log.d(TAG, "SQLCipher libs loaded.");

        // Inizializza il FileManager. Un'unica istanza di FileManager è creata e resa disponibile
        // per l'intera applicazione. Questo è utile per gestire file cifrati in modo centralizzato.
        fileManager = new FileManager(getApplicationContext());
        Log.d(TAG, "FileManager initialized.");
    }

    /**
     * Fornisce un'istanza singleton di {@link FileManager} all'applicazione.
     * Altri componenti (Activity, Fragment, Services) possono richiamare questo metodo
     * per ottenere l'istanza di FileManager e interagire con le operazioni sui file.
     *
     * @return L'istanza di {@link FileManager} per l'applicazione.
     */
    public FileManager getFileManager() {
        return fileManager;
    }

    /**
     * Chiamato quando l'applicazione sta per essere terminata dal sistema.
     * È il luogo ideale per rilasciare risorse e pulire file temporanei.
     * Qui viene chiamato {@code cleanTempFiles()} di {@link FileManager}.
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        // Assicura di chiamare cleanTempFiles o shutdown del FileManager quando l'app termina
        if (fileManager != null) {
            fileManager.cleanTempFiles(); // Pulisce i file temporanei decifrati
            Log.d(TAG, "FileManager temp files cleaned on terminate.");
        }
        Log.d(TAG, "Application terminated.");
    }
}