package com.example.securenotesapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * {@code KeyManager} è responsabile della gestione di tutte le chiavi crittografiche
 * utilizzate nell'applicazione SecureNotes, incluse le passphrase per il database SQLCipher,
 * le chiavi per i file di backup e la gestione sicura dei PIN utente.
 * Utilizza Android Keystore e EncryptedSharedPreferences per archiviare le chiavi in modo sicuro.
 */
public class KeyManager {

    private static final String TAG = "KeyManager";
    // Alias per la chiave maestra che Android Keystore userà per proteggere la passphrase del database.
    private static final String KEY_ALIAS = "secure_notes_db_key";
    // Nome del file per le EncryptedSharedPreferences.
    private static final String ENCRYPTED_PREFS_NAME = "secure_notes_prefs";
    // Chiave per salvare la passphrase del database all'interno di EncryptedSharedPreferences.
    private static final String PASSPHRASE_PREF_KEY = "db_passphrase"; // Chiave per la passphrase salvata
    // --- Costanti per la gestione del PIN ---
    // Chiave per salvare l'hash del PIN dell'utente.
    private static final String PIN_PREF_KEY = "user_pin_hash";
    // Chiave per salvare il salt associato all'hash del PIN.
    private static final String PIN_SALT_PREF_KEY = "user_pin_salt";
    // Numero di iterazioni per l'algoritmo PBKDF2 per generare l'hash del PIN. Un valore più alto aumenta la sicurezza ma anche il tempo di calcolo.
    private static final int PBKDF2_ITERATIONS = 10000;
    // Lunghezza in bit della chiave derivata (hash) dal PIN.
    private static final int KEY_LENGTH = 256; // Lunghezza della chiave derivata in bit
    // ---------------------------------------------


    private Context context;
    // Oggetto EncryptedSharedPreferences per archiviare dati sensibili in modo cifrato.
    private SharedPreferences encryptedSharedPreferences;
    // Nome della directory dove FileManager salva i file criptati.
    private static final String ENCRYPTED_FILES_DIR = "encrypted_files";

    /**
     * Costruttore per {@code KeyManager}.
     * Inizializza il contesto dell'applicazione e configura {@link EncryptedSharedPreferences}.
     * {@link EncryptedSharedPreferences} gestisce autonomamente la creazione e l'uso di una master key
     * attraverso Android Keystore per cifrare i dati.
     *
     * @param context Il contesto dell'applicazione.
     * @throws RuntimeException Se si verifica un errore durante l'inizializzazione di EncryptedSharedPreferences.
     */
    public KeyManager(Context context) {
        this.context = context.getApplicationContext();
        try {
            // Ottiene o crea una chiave master AES256_GCM dal Keystore Android.
            // Questa chiave viene usata internamente da EncryptedSharedPreferences.
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            // Inizializza EncryptedSharedPreferences, specificando lo schema di cifratura per chiavi e valori.
            encryptedSharedPreferences = EncryptedSharedPreferences.create(
                    ENCRYPTED_PREFS_NAME, // Nome del file delle preferenze
                    masterKeyAlias,// Alias della chiave master
                    context,// Contesto dell'applicazione
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,// Schema di cifratura per le chiavi delle preferenze
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM// Schema di cifratura per i valori delle preferenze
            );
            Log.d(TAG, "EncryptedSharedPreferences inizializzato con successo.");

        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Errore durante l'inizializzazione di EncryptedSharedPreferences: " + e.getMessage());
            throw new RuntimeException("Impossibile inizializzare EncryptedSharedPreferences", e);
        }
    }
    /**
     * Genera una chiave AES robusta ({@link SecretKeySpec}) da una password fornita dall'utente
     * utilizzando l'algoritmo PBKDF2WithHmacSHA256. Questa chiave è destinata
     * alla cifratura/decifratura del file di backup.
     *
     * @param password La password in chiaro fornita dall'utente.
     * @return Un oggetto {@link SecretKeySpec} per AES.
     * @throws NoSuchAlgorithmException Se l'algoritmo "PBKDF2WithHmacSHA256" non è disponibile.
     * @throws InvalidKeySpecException Se la specifica della chiave non è valida.
     */
    public SecretKeySpec generateAesKeyFromPassword(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Un salt fisso viene usato per il backup. In scenari più complessi, un salt dinamico sarebbe preferibile,
        // ma per il backup dell'utente con password, un salt fisso è accettabile.
        byte[] salt = "SecureNotesBackupSalt".getBytes(); // Un salt fisso
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        // Specifica per PBKDF2: password, salt, numero di iterazioni e lunghezza della chiave.
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);// 65536 iterazioni, chiave AES-256 (256 bit).
        SecretKey tmp = factory.generateSecret(spec);// Genera la chiave segreta temporanea.
        return new SecretKeySpec(tmp.getEncoded(), "AES"); // Crea una SecretKeySpec per AES.
    }

    /**
     * Genera un Initialization Vector (IV) casuale di 12 byte, come raccomandato per la modalità AES/GCM.
     * L'IV è necessario per garantire che la stessa chiave produca output crittografici diversi per lo stesso input.
     *
     * @return Un array di byte contenente l'IV generato.
     */
    public byte[] generateIv() {
        // GCM raccomanda un IV di 12 byte. Possiamo generarlo casualmente.
        byte[] iv = new byte[12]; // IV di 12 byte per GCM
        new SecureRandom().nextBytes(iv); // Popola l'IV con byte casuali sicuri.
        return iv;
    }

    /**
     * Restituisce un oggetto {@link File} che rappresenta il percorso di un file crittografato locale.
     * I file crittografati sono memorizzati in una sottocartella specifica definita da {@code ENCRYPTED_FILES_DIR}.
     * Se la directory non esiste, viene creata.
     *
     * @param context Contesto dell'applicazione.
     * @param filename Il nome del file (che sarà anche il nome del file crittografato).
     * @return Un oggetto {@link File} che punta al percorso del file crittografato.
     */
    public File getEncryptedFile(Context context, String filename) {
        // Ottiene il percorso della directory per i file criptati.
        File encryptedFilesDir = new File(context.getFilesDir(), ENCRYPTED_FILES_DIR);
        if (!encryptedFilesDir.exists()) {
            encryptedFilesDir.mkdirs(); // Crea la directory se non esiste
        }
        return new File(encryptedFilesDir, filename);
    }

    /**
     * Genera una nuova passphrase di 32 byte (256 bit) per il database SQLCipher
     * e la memorizza in modo cifrato utilizzando {@link EncryptedSharedPreferences}.
     * Questo metodo dovrebbe essere chiamato solo la prima volta che il database viene creato.
     *
     * @return La passphrase appena generata come array di byte.
     * @throws Exception Se la generazione o il salvataggio della passphrase falliscono.
     */
    public byte[] generateAndStoreNewPassphrase() throws Exception {
        byte[] passphrase = new byte[32]; // 256 bit = 32 byte per la passphrase di SQLCipher.
        new SecureRandom().nextBytes(passphrase); // Genera byte casuali crittograficamente sicuri.

        // Converte l'array di byte della passphrase in una stringa Base64 per la memorizzazione in SharedPreferences.
        String encodedPassphrase = Base64.encodeToString(passphrase, Base64.DEFAULT);

        // Salva la passphrase cifrata in EncryptedSharedPreferences.
        encryptedSharedPreferences.edit()
                .putString(PASSPHRASE_PREF_KEY, encodedPassphrase)
                .apply();// Applica le modifiche in modo asincrono.


        Log.d(TAG, "Nuova passphrase generata e salvata cifrata.");
        return passphrase;
    }

    /**
     * Recupera la passphrase cifrata da {@link EncryptedSharedPreferences} e la decifra.
     * Questo metodo decodifica la stringa Base64 e restituisce l'array di byte della passphrase.
     *
     * @return La passphrase decifrata come array di byte, o null se non è stata trovata.
     */
    public byte[] retrievePassphrase() {
        String encodedPassphrase = encryptedSharedPreferences.getString(PASSPHRASE_PREF_KEY, null);
        if (encodedPassphrase != null) {
            byte[] passphrase = Base64.decode(encodedPassphrase, Base64.DEFAULT);// Decodifica la stringa Base64.
            Log.d(TAG, "Passphrase recuperata e decifrata.");
            return passphrase;
        }
        Log.d(TAG, "Passphrase non trovata in EncryptedSharedPreferences.");
        return null;
    }

    /**
     * Verifica se la passphrase del database esiste già in {@link EncryptedSharedPreferences}.
     * Questo è utile per determinare se il database è già stato inizializzato e protetto da una passphrase.
     *
     * @return true se la passphrase esiste, false altrimenti.
     */
    public boolean hasPassphrase() {
        return encryptedSharedPreferences.contains(PASSPHRASE_PREF_KEY);
    }

    /**
     * Elimina la passphrase del database da {@link EncryptedSharedPreferences}.
     * Questo può essere utile in scenari di reset o logout.
     */
    public void clearPassphrase() {
        encryptedSharedPreferences.edit().remove(PASSPHRASE_PREF_KEY).apply();
        Log.d(TAG, "Passphrase rimossa da EncryptedSharedPreferences.");
    }
    // ---  METODI PER LA GESTIONE DEL PIN ---

    /**
     * Genera un hash sicuro del PIN fornito dall'utente utilizzando l'algoritmo PBKDF2WithHmacSHA256
     * con un salt casuale e lo salva insieme al salt in {@link EncryptedSharedPreferences}.
     *
     * @param pin Il PIN testuale (in chiaro) da hashare e salvare.
     * @return true se il PIN è stato hashato e salvato con successo, false altrimenti.
     */
    public boolean savePin(String pin) {
        if (pin == null || pin.isEmpty()) {
            Log.e(TAG, "Tentativo di salvare un PIN nullo o vuoto.");
            return false;
        }
        try {
            // 1. Genera un salt casuale e univoco per ogni PIN per prevenire attacchi a dizionario/rainbow table.
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16]; // 16 byte = 128 bit di salt
            random.nextBytes(salt);

            // 2. Deriva la chiave (hash) dal PIN e dal salt usando PBKDF2
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
            SecretKey secret = skf.generateSecret(spec);
            byte[] hash = secret.getEncoded();// Ottiene l'hash come array di byte.

            // 3. Salva l'hash e il salt (entrambi convertiti in stringhe Base64) in EncryptedSharedPreferences.
            String encodedHash = Base64.encodeToString(hash, Base64.DEFAULT);
            String encodedSalt = Base64.encodeToString(salt, Base64.DEFAULT);

            encryptedSharedPreferences.edit()
                    .putString(PIN_PREF_KEY, encodedHash)
                    .putString(PIN_SALT_PREF_KEY, encodedSalt)
                    .apply();// Applica le modifiche.

            Log.d(TAG, "PIN hashato e salt salvati con successo.");
            return true;

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, "Errore durante il salvataggio del PIN: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Verifica se un PIN fornito dall'utente corrisponde all'hash del PIN salvato.
     * Il PIN fornito viene hashato con il salt salvato e confrontato con l'hash memorizzato.
     *
     * @param pin Il PIN testuale (in chiaro) fornito dall'utente per la verifica.
     * @return true se il PIN corrisponde, false altrimenti.
     */
    public boolean verifyPin(String pin) {
        if (pin == null || pin.isEmpty()) {
            Log.d(TAG, "Tentativo di verificare un PIN nullo o vuoto.");
            return false;
        }

        String storedHash = encryptedSharedPreferences.getString(PIN_PREF_KEY, null);
        String storedSalt = encryptedSharedPreferences.getString(PIN_SALT_PREF_KEY, null);

        if (storedHash == null || storedSalt == null) {
            Log.d(TAG, "Nessun PIN salvato per la verifica.");
            return false; // Nessun PIN è stato impostato
        }

        try {
            byte[] salt = Base64.decode(storedSalt, Base64.DEFAULT);// Decodifica il salt salvato.
            byte[] hashToCompare = Base64.decode(storedHash, Base64.DEFAULT);// Decodifica l'hash salvato.

            // Deriva l'hash dal PIN fornito e dal salt recuperato.
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
            SecretKey secret = skf.generateSecret(spec);
            byte[] enteredPinHash = secret.getEncoded(); // Hash del PIN inserito.

            // Confronta l'hash generato con l'hash salvato
            // Utilizza Arrays.equals per un confronto sicuro
            return java.util.Arrays.equals(enteredPinHash, hashToCompare);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, "Errore durante la verifica del PIN: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Verifica se un PIN è già stato impostato (ovvero, se l'hash e il salt del PIN sono presenti
     * in {@link EncryptedSharedPreferences}).
     *
     * @return true se un PIN è presente, false altrimenti.
     */
    public boolean hasPin() {
        return encryptedSharedPreferences.contains(PIN_PREF_KEY) &&// Controlla se l'hash del PIN esiste.
                encryptedSharedPreferences.contains(PIN_SALT_PREF_KEY); // Controlla se il salt del PIN esiste.
    }

    /**
     * Elimina l'hash del PIN e il salt associato da {@link EncryptedSharedPreferences}.
     * Questo rimuove il PIN impostato dall'utente.
     */
    public void clearPin() {
        encryptedSharedPreferences.edit()
                .remove(PIN_PREF_KEY)// Rimuove la chiave dell'hash.
                .remove(PIN_SALT_PREF_KEY)// Rimuove la chiave del salt.
                .apply();// Applica le modifiche.
        Log.d(TAG, "PIN e salt rimossi.");
    }
    // --- FINE METODI PER LA GESTIONE DEL PIN ---
}