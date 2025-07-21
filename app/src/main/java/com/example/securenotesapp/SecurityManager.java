package com.example.securenotesapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64; //  Base64 per codificare/decodificare byte[]
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom; // SecureRandom per la generazione sicura della chiave

/**
 * {@code SecurityManager} gestisce la configurazione e l'accesso sicuro a componenti
 * crittografici fondamentali dell'applicazione, come {@link EncryptedSharedPreferences}
 * e la generazione/recupero della passphrase del database.
 * Questo manager facilita l'interazione con Android Keystore per la protezione delle chiavi sensibili.
 *
 * Nota: questa classe sembra avere funzionalità simili a {@code KeyManager}. Potrebbe essere utile
 * consolidare o distinguere chiaramente i ruoli per evitare ridondanze.
 */
public class SecurityManager {
    // Nome del file per le SharedPreferences cifrate.
    private static final String PREF_FILE_NAME = "secure_settings";
    // Nome della directory per i file sicuri (usato da FileManager, non direttamente qui).
    public static final String FILE_DIR_NAME = "secure_files";
    // Alias per la master key gestita da Android Keystore.
    private static final String MASTER_KEY_ALIAS = "secure_notes_master_key";
    // Chiave per archiviare la passphrase del database all'interno delle SharedPreferences cifrate.
    private static final String DATABASE_PASSPHRASE_KEY = "db_passphrase";

    private Context context;
    // L'istanza di EncryptedSharedPreferences per archiviare dati sensibili in modo cifrato.
    private SharedPreferences encryptedSharedPreferences;
    // L'alias della master key generato o recuperato da Android Keystore
    private String masterKeyAlias;
    /**
     * Costruttore per {@code SecurityManager}.
     * Inizializza il contesto e avvia il processo di inizializzazione dei componenti di sicurezza.
     *
     * @param context Il contesto dell'applicazione.
     */
    public SecurityManager(Context context) {
        this.context = context;
        initializeSecurityComponents();
    }

    /**
     * Inizializza i componenti di sicurezza, in particolare {@link EncryptedSharedPreferences}.
     * Questo include la generazione o il recupero della master key da Android Keystore.
     * Gestisce le eccezioni legate alla sicurezza e all'I/O.
     */
    private void initializeSecurityComponents() {
        try {
            // Ottiene o crea una master key sicura utilizzando MasterKeys.AES256_GCM_SPEC.
            // Questa chiave è protetta da Android Keystore
            masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            // Crea un'istanza di EncryptedSharedPreferences per archiviare dati in modo cifrato.
            // Utilizza lo schema di cifratura AES256_SIV per le chiavi delle preferenze
            // e AES256_GCM per i valori delle preferenze.
            encryptedSharedPreferences = EncryptedSharedPreferences.create(
                    PREF_FILE_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public SharedPreferences getEncryptedSharedPreferences() {
        return encryptedSharedPreferences;
    }

    public EncryptedFile getEncryptedFile(String fileName) throws GeneralSecurityException, IOException {
        if (masterKeyAlias == null) {
            throw new IllegalStateException("Security components not initialized. Master key is null.");
        }

        File filesDir = new File(context.getFilesDir(), FILE_DIR_NAME);
        if (!filesDir.exists()) {
            filesDir.mkdirs();
        }

        File file = new File(filesDir, fileName);

        return new EncryptedFile.Builder(
                file,
                context,
                masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();
    }

    // Metodo per testare il salvataggio e recupero di una stringa criptata
    public void saveTestSetting(String key, String value) {
        if (encryptedSharedPreferences != null) {
            encryptedSharedPreferences.edit().putString(key, value).apply();
        }
    }

    public String loadTestSetting(String key) {
        if (encryptedSharedPreferences != null) {
            return encryptedSharedPreferences.getString(key, "Default Value");
        }
        return "Error: SharedPreferences not initialized";
    }

    /**
     * Recupera la passphrase del database da {@link EncryptedSharedPreferences}.
     * Se la passphrase non esiste, ne genera una nuova di 32 byte (256 bit)
     * e la memorizza in modo cifrato prima di restituirla.
     *
     * @return La passphrase del database come array di byte.
     * @throws GeneralSecurityException se si verifica un errore crittografico.
     * @throws IOException se c'è un errore I/O nella gestione della chiave.
     */
    public byte[] getDatabasePassphrase() throws GeneralSecurityException, IOException {
        // Verifica che EncryptedSharedPreferences sia stato inizializzato.
        if (encryptedSharedPreferences == null) {
            throw new IllegalStateException("EncryptedSharedPreferences non inizializzate.");
        }

        // Tenta di recuperare la passphrase codificata in Base64.
        String encodedPassphrase = encryptedSharedPreferences.getString(DATABASE_PASSPHRASE_KEY, null);

        if (encodedPassphrase == null) {
            // Se la passphrase non è stata trovata, significa che è il primo avvio o è stata resettata.
            // Genera una nuova passphrase sicura di 32 byte (256 bit).
            byte[] newPassphrase = generateSecureRandomBytes(32); // 32 bytes per AES256
            // Codifica la nuova passphrase in Base64 per memorizzarla come stringa.
            String newEncodedPassphrase = Base64.encodeToString(newPassphrase, Base64.NO_WRAP);

            // Salva la nuova passphrase cifrata in EncryptedSharedPreferences.
            encryptedSharedPreferences.edit()
                    .putString(DATABASE_PASSPHRASE_KEY, newEncodedPassphrase)
                    .apply();

            return newPassphrase;// Restituisce la nuova passphrase
        } else {
            // Se la passphrase esiste, la decodifica da Base64 e la restituisce
            return Base64.decode(encodedPassphrase, Base64.NO_WRAP);
        }
    }

    /**
     * Genera un array di byte casuali e sicuri utilizzando {@link SecureRandom}.
     * Questo è fondamentale per creare chiavi crittografiche o salt.
     *
     * @param size La dimensione desiderata dell'array di byte.
     * @return L'array di byte generato in modo sicuro.
     */
    private byte[] generateSecureRandomBytes(int size) {
        SecureRandom secureRandom = new SecureRandom();// Crea un generatore di numeri casuali crittograficamente sicuro.
        byte[] bytes = new byte[size];// Crea un array di byte della dimensione specificata.
        secureRandom.nextBytes(bytes);// Popola l'array con byte casuali.
        return bytes;
    }
}