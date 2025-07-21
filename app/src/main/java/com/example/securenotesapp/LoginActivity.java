package com.example.securenotesapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.securenotesapp.database.NoteDatabase;

import java.util.concurrent.Executor;

/**
 * {@code LoginActivity} è la schermata di login principale dell'applicazione SecureNotes.
 * Gestisce l'autenticazione dell'utente tramite biometria (impronta digitale o riconoscimento facciale)
 * o tramite un PIN di fallback. Dopo l'autenticazione riuscita, inizializza il database
 * e naviga alla schermata principale dell'applicazione.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "SecureNotes_Login";
    private Executor executor;// Esecutore per i callback biometrici, usa il thread principale.
    private BiometricPrompt biometricPrompt;// Oggetto per gestire il flusso di autenticazione biometrica.
    private BiometricPrompt.PromptInfo promptInfo;// Informazioni visualizzate nel prompt biometrico.

    private KeyManager keyManager; // Gestore per le chiavi di sicurezza, incluso l'hash del PIN e la passphrase del DB.

    // Elementi UI per la gestione del PIN di fallback
    private LinearLayout pinFallbackLayout;
    private EditText editTextLoginPin;
    private Button buttonVerifyPin;
    private Button buttonAuthenticateBiometric; // Pulsante per avviare l'autenticazione biometrica
    private TextView loginMessageText;

    /**
     * Chiamato quando l'activity viene creata.
     * Inizializza gli elementi UI, il {@link KeyManager} e configura il {@link BiometricPrompt}.
     * Avvia la verifica della disponibilità biometrica e tenta l'autenticazione.
     *
     * @param savedInstanceState Se l'activity viene ricreata, questo è il Bundle che contiene i dati precedentemente salvati.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        keyManager = new KeyManager(this); // Inizializza il KeyManager

        // Inizializza gli elementi UI del PIN
        pinFallbackLayout = findViewById(R.id.pin_fallback_layout);
        editTextLoginPin = findViewById(R.id.edit_text_login_pin); // Assicurati che l'ID corrisponda al tuo XML
        buttonVerifyPin = findViewById(R.id.button_verify_pin);     // Assicurati che l'ID corrisponda al tuo XML

        // Inizializza il pulsante per l'autenticazione biometrica (potrebbe essere inizialmente nascosto)
        buttonAuthenticateBiometric = findViewById(R.id.button_authenticate); // L'ID corretto dal XML
        buttonAuthenticateBiometric.setOnClickListener(v -> showBiometricPrompt());


        // Imposta il listener per il pulsante di verifica del PIN
        buttonVerifyPin.setOnClickListener(v -> verifyPin());

        loginMessageText = findViewById(R.id.login_message_text);

        // Ottiene un esecutore per il thread principale, dove verranno eseguiti i callback biometrici.
        executor = ContextCompat.getMainExecutor(this);

        // Inizializza il BiometricPrompt con un AuthenticationCallback per gestire i risultati dell'autenticazione.
        biometricPrompt = new BiometricPrompt(LoginActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {

            /**
             * Chiamato quando si verifica un errore durante l'autenticazione biometrica.
             * @param errorCode Il codice di errore.
             * @param errString Il messaggio di errore leggibile dall'utente.
             */
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Log.e(TAG, "Errore autenticazione biometrica: " + errString + " (Codice: " + errorCode + ")");
                Toast.makeText(getApplicationContext(), "Errore biometrico: " + errString, Toast.LENGTH_SHORT).show();

                // Mostra l'UI del PIN di fallback in caso di errore, a meno che l'utente non abbia annullato o ci sia un lockout permanente
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) { // Cliccato "Usa PIN di fallback"
                    // L'utente ha annullato o cliccato il pulsante negativo, mostra il PIN di fallback
                    showPinFallbackUI("Autenticazione biometrica annullata dall'utente.");
                } else if (errorCode == BiometricPrompt.ERROR_LOCKOUT ||
                        errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                    // Lockout: biometrica disabilitata temporaneamente o permanentemente. Mostra PIN.
                    showPinFallbackUI("Troppi tentativi biometrici falliti. Usa il PIN.");
                }
                else {
                    // Per altri errori, mostra il PIN di fallback
                    showPinFallbackUI("Errore biometrico: " + errString);
                }
            }

            /**
             * Chiamato quando l'autenticazione biometrica ha successo.
             * @param result L'oggetto che contiene il risultato dell'autenticazione.
             */
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Log.d(TAG, "Autenticazione biometrica riuscita!");
                Toast.makeText(getApplicationContext(), "Autenticazione riuscita!", Toast.LENGTH_SHORT).show();
                // Se l'autenticazione biometrica ha successo, procedi all'Activity principale
                initializeDatabaseAndNavigate(); // Chiamata per inizializzare DB e navigare
            }

            /**
             * Chiamato quando l'autenticazione biometrica fallisce (es. impronta non riconosciuta).
             */
            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Log.d(TAG, "Autenticazione biometrica fallita.");
                Toast.makeText(getApplicationContext(), "Autenticazione fallita. Riprova o usa il PIN.", Toast.LENGTH_SHORT).show();
                // Mostra l'UI del PIN di fallback se la biometria fallisce
                showPinFallbackUI("Autenticazione biometrica fallita.");
            }
        });

        // Configura le informazioni del prompt biometrico.
        // Utilizza BIOMETRIC_STRONG e BIOMETRIC_WEAK per consentire diversi livelli di sicurezza biometrica.
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticazione richiesta")
                .setSubtitle("Accedi a SecureNotes")
                .setNegativeButtonText("Usa PIN di fallback") // Testo per il pulsante di annullamento/fallback
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG | // Preferisce biometria forte  (es. impronta su hardware sicuro).
                                BiometricManager.Authenticators.BIOMETRIC_WEAK    // Accetta anche biometria debole (es. riconoscimento facciale non 3D).
                )
                .build();


        // Avvia la verifica della disponibilità della biometria e l'autenticazione
        checkBiometricAvailabilityAndAuthenticate();

        // Gestione del pulsante Indietro per impedire di uscire senza autenticazione
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(LoginActivity.this, "Per favore, autenticati per accedere all'app.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Controlla la disponibilità dell'autenticazione biometrica sul dispositivo
     * e decide se avviare il prompt biometrico o mostrare l'UI del PIN di fallback.
     */
    private void checkBiometricAvailabilityAndAuthenticate() {
        BiometricManager biometricManager = BiometricManager.from(this);
        // Controlla se è possibile autenticarsi con biometria forte o debole.
        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
        );

        // Gestisce i diversi stati di disponibilità della biometria.
        switch (canAuthenticate) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.d(TAG, "L'autenticazione biometrica è disponibile e configurata.");
                // Se la biometria è disponibile, nasconde l'UI del PIN e mostra/usa il pulsante biometria
                pinFallbackLayout.setVisibility(View.GONE);
                buttonAuthenticateBiometric.setVisibility(View.VISIBLE);
                showBiometricPrompt(); // Avvia subito il prompt biometrico
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Log.e(TAG, "Nessun hardware biometrico disponibile.");
                Toast.makeText(this, "Nessun hardware biometrico disponibile. Usa il PIN.", Toast.LENGTH_LONG).show();
                showPinFallbackUI("Nessun hardware biometrico disponibile.");// Mostra il PIN
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Log.e(TAG, "Hardware biometrico non disponibile o occupato.");
                Toast.makeText(this, "Hardware biometrico non disponibile. Riprova o usa il PIN.", Toast.LENGTH_LONG).show();
                showPinFallbackUI("Hardware biometrico non disponibile.");// Mostra il PIN
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Log.e(TAG, "Nessuna impronta digitale o volto registrato.");
                Toast.makeText(this, "Nessuna biometria registrata. Usa il PIN di fallback o configura la biometria nelle impostazioni.", Toast.LENGTH_LONG).show();
                showPinFallbackUI("Nessuna biometria registrata.");// Mostra il PIN
                break;
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                Log.e(TAG, "Aggiornamento di sicurezza richiesto.");
                Toast.makeText(this, "Aggiornamento di sicurezza richiesto per la biometria. Usa il PIN.", Toast.LENGTH_LONG).show();
                showPinFallbackUI("Aggiornamento di sicurezza richiesto.");// Mostra il PIN
                break;
            case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
                Log.e(TAG, "Biometria non supportata.");
                Toast.makeText(this, "Biometria non supportata sul dispositivo. Usa il PIN.", Toast.LENGTH_LONG).show();
                showPinFallbackUI("Biometria non supportata.");// Mostra il PIN
                break;
            default:
                Log.e(TAG, "Stato biometrico sconosciuto: " + canAuthenticate);
                Toast.makeText(this, "Errore biometrico sconosciuto. Usa il PIN.", Toast.LENGTH_LONG).show();
                showPinFallbackUI("Errore biometrico sconosciuto.");// Mostra il PIN
                break;
        }
    }

    /**
     * Avvia la visualizzazione del prompt di autenticazione biometrica.
     */
    private void showBiometricPrompt() {
        biometricPrompt.authenticate(promptInfo);
    }

    /**
     * Mostra l'interfaccia utente per l'inserimento del PIN di fallback.
     * Questa funzione viene chiamata quando l'autenticazione biometrica non è possibile o fallisce.
     *
     * @param reason Una stringa che descrive il motivo per cui è stato mostrato il fallback al PIN (per debug/log).
     */
    private void showPinFallbackUI(String reason) {
        Log.d(TAG, "Mostrando UI PIN di fallback. Motivo: " + reason);
        // Nasconde il messaggio di login iniziale
        if (loginMessageText != null) {
            loginMessageText.setVisibility(View.GONE);
        }
        // Nasconde il pulsante di autenticazione biometrica
        buttonAuthenticateBiometric.setVisibility(View.GONE);
        // Mostra il layout del PIN
        pinFallbackLayout.setVisibility(View.VISIBLE);

        // Se non è stato impostato un PIN, avvisa l'utente che deve crearne uno
        if (!keyManager.hasPin()) {
            Toast.makeText(this, "Nessun PIN di fallback impostato. Vai alle impostazioni per crearne uno.", Toast.LENGTH_LONG).show();
        }
        // Pulisci il campo del PIN quando viene mostrato
        editTextLoginPin.setText("");
    }

    /**
     * Verifica il PIN inserito dall'utente.
     * Questo metodo recupera il PIN dal campo di testo e lo confronta con l'hash del PIN salvato
     * utilizzando il {@link KeyManager}.
     */
    private void verifyPin() {
        String enteredPin = editTextLoginPin.getText().toString();// Ottiene il testo dal campo PIN.

        if (enteredPin.isEmpty()) {
            Toast.makeText(this, "Per favore, inserisci il PIN.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Controlla se un PIN è stato effettivamente impostato.
        if (!keyManager.hasPin()) {
            Toast.makeText(this, "Nessun PIN di fallback impostato. Vai alle impostazioni per crearne uno.", Toast.LENGTH_LONG).show();
            return;
        }

        // Verifica il PIN tramite il KeyManager.
        if (keyManager.verifyPin(enteredPin)) {
            Toast.makeText(this, "PIN corretto. Accesso consentito.", Toast.LENGTH_SHORT).show();
            initializeDatabaseAndNavigate(); // PIN corretto, inizializza DB e vai alla MainActivity
        } else {
            Toast.makeText(this, "PIN errato. Riprova.", Toast.LENGTH_SHORT).show();
            editTextLoginPin.setText(""); // Pulisci il campo in caso di errore
        }
    }

    /**
     * Gestisce il recupero o la generazione della passphrase del database SQLCipher
     * e l'inizializzazione del {@link NoteDatabase}.
     * Questo metodo viene chiamato dopo un'autenticazione riuscita (biometrica o PIN).
     */
    private void initializeDatabaseAndNavigate() {
        Log.d(TAG, "Inizializzazione database e navigazione.");
        try {
            byte[] passphrase;
            if (keyManager.hasPassphrase()) {
                // Se la passphrase esiste, la recupera
                passphrase = keyManager.retrievePassphrase();
                Log.d(TAG, "Passphrase esistente recuperata.");
            } else {
                // Se non esiste, è il primo avvio o è stata cancellata.
                // Genera e memorizza una nuova passphrase.
                passphrase = keyManager.generateAndStoreNewPassphrase();
                Log.d(TAG, "Nuova passphrase generata e salvata cifrata.");
            }

            if (passphrase != null) {
                // Inizializza il database Room con la passphrase recuperata/generata
                NoteDatabase.getDatabase(getApplicationContext(), passphrase);
                Log.d(TAG, "Database inizializzato con successo.");

                // Dopo l'inizializzazione del database, controlla se è il primo avvio e il PIN non è impostato.
                // Viene mostrato un prompt per incoraggiare l'utente a impostare un PIN.
                if (!keyManager.hasPin() && !getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean("pin_setup_prompt_shown", false)) {
                    showPinSetupPrompt();
                    // Segna che il prompt è stato mostrato per non riproporlo al prossimo avvio (se l'utente lo salta)
                    getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putBoolean("pin_setup_prompt_shown", true).apply();
                } else {
                    navigateToMainActivity(); // Vai alla MainActivity se il PIN è già impostato o il prompt è già stato mostrato
                }

            } else {
                // Gestione di un caso critico in cui la passphrase non può essere recuperata o generata.
                Log.e(TAG, "Passphrase è null. Impossibile inizializzare il database.");
                Toast.makeText(this, "Errore critico: impossibile recuperare la chiave di sicurezza.", Toast.LENGTH_LONG).show();
                finish(); // Chiudi l'app in caso di errore critico
            }

        } catch (Exception e) {
            Log.e(TAG, "Errore nella gestione della passphrase o inizializzazione del database: " + e.getMessage(), e);
            Toast.makeText(this, "Errore di sicurezza o database. Riprova.", Toast.LENGTH_LONG).show();
            finish(); // In caso di errore critico, chiudi l'app
        }
    }

    /**
     * Mostra un {@link AlertDialog} all'utente per suggerire l'impostazione di un PIN.
     * Questo prompt appare al primo avvio se l'utente non ha ancora impostato un PIN.
     */
    private void showPinSetupPrompt() {
        new AlertDialog.Builder(this)
                .setTitle("Imposta PIN di Sicurezza")
                .setMessage("È la prima volta che usi l'app o la passphrase è stata resettata. Per maggiore sicurezza e come metodo di accesso alternativo, ti consigliamo di impostare un PIN di fallback nelle impostazioni.")
                .setPositiveButton("Vai alle Impostazioni", (dialog, which) -> {
                    // Crea un intent per navigare alla SettingsActivity dove l'utente può impostare il PIN.
                    Intent settingsIntent = new Intent(LoginActivity.this, SettingsActivity.class);
                    startActivity(settingsIntent);
                    // Non finish() qui, l'utente potrebbe tornare indietro.
                })
                .setNegativeButton("Continua", (dialog, which) -> {
                    // Se l'utente sceglie di non impostare un PIN per ora, naviga comunque alla MainActivity
                    navigateToMainActivity();
                })
                .setCancelable(false) // Impedisce di chiudere il dialog senza una scelta
                .show();
    }

    /**
     * Naviga dall'attuale {@code LoginActivity} alla {@code MainActivity} e chiude la {@code LoginActivity}.
     * Chiudere l'activity di login è importante per impedire all'utente di tornare indietro senza autenticazione.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Chiudi LoginActivity in modo che l'utente non possa tornare indietro con il tasto back
    }

    /**
     * (Opzionale) Metodo per reindirizzare l'utente alle impostazioni di sicurezza del dispositivo
     * se la biometria non è configurata.
     */
    private void promptForSecuritySettings() {
        Intent enrollIntent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
        startActivity(enrollIntent);
    }
}