package com.example.securenotesapp;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKeys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@code FileManager} gestisce le operazioni sui file all'interno dell'applicazione,
 * inclusa la crittografia, decrittografia, salvataggio, apertura ed eliminazione di file.
 * Utilizza {@link EncryptedFile} e {@link MasterKeys} di Jetpack Security per la sicurezza dei dati locali.
 */
public class FileManager {
    private static final String TAG = "FileManager";
    // Nome della directory dove verranno salvati i file criptati nell'archiviazione interna dell'app.
    public static final String ENCRYPTED_FILES_DIR = "encrypted_files";
    // Directory per i file temporanei decifrati che vengono aperti dall'utente.
    private static final String TEMP_FILES_DIR = "temp"; // Directory per i file temporanei decifrati

    private Context context;
    private String masterKeyAlias;// Alias della chiave master utilizzata per la crittografia.
    public ExecutorService fileIOExecutor; // Executor per eseguire operazioni di I/O su file in background.

    /**
     * Costruttore per {@code FileManager}.
     * Inizializza il contesto e genera o recupera la chiave master per la crittografia.
     * Inizializza anche un SingleThreadExecutor per gestire le operazioni di I/O sui file.
     *
     * @param context Il contesto dell'applicazione.
     * @throws RuntimeException Se si verifica un errore durante la creazione della master key.
     */
    public FileManager(Context context) {
        this.context = context;
        try {
            // Ottiene o crea una chiave master AES256_GCM per la crittografia simmetrica dei file.
            this.masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Errore nella creazione della master key", e);
            throw new RuntimeException("Impossibile inizializzare FileManager", e);
        }
        // Inizializza l'executor per le operazioni sui file, usando un singolo thread per serializzare le operazioni.
        fileIOExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Cripta un flusso di input e lo salva come file criptato nella directory interna dell'app.
     * Viene generato un nome di file univoco.
     *
     * @param inputStream Il flusso di input da cui leggere i dati da criptare.
     * @return Il percorso relativo del file criptato salvato.
     * @throws IOException Se si verifica un errore di I/O durante la lettura o scrittura.
     * @throws GeneralSecurityException Se si verifica un errore durante la crittografia.
     */
    public String encryptAndSaveFile(InputStream inputStream) throws IOException, GeneralSecurityException {
        // Crea la directory per i file criptati se non esiste
        File encryptedFilesDir = new File(context.getFilesDir(), ENCRYPTED_FILES_DIR);
        if (!encryptedFilesDir.exists()) {
            encryptedFilesDir.mkdirs();// Crea tutte le directory necessarie.
        }

        // Genera un nome di file univoco per il file criptato per evitare collisioni.
        String encryptedFileName = UUID.randomUUID().toString() + ".encrypted";
        File encryptedFile = new File(encryptedFilesDir, encryptedFileName);

        // Crea un'istanza di EncryptedFile.Builder per configurare il file criptato.
        EncryptedFile encryptedFileWriter = new EncryptedFile.Builder(
                encryptedFile,
                context,
                masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        // Apre un OutputStream criptato e scrive i dati dal flusso di input
        try (OutputStream outputStream = encryptedFileWriter.openFileOutput()) {
            byte[] buffer = new byte[1024];// Buffer per leggere i dati a blocchi.
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);// Scrive i dati criptati.
            }
            outputStream.flush(); // Assicura che tutti i dati siano scritti
        }
        Log.d(TAG, "File criptato salvato in: " + encryptedFile.getAbsolutePath());
        // Restituisce il percorso relativo per un salvataggio più flessibile (es. nel database).
        return ENCRYPTED_FILES_DIR + File.separator + encryptedFileName;
    }

    /**
     * Decifra un file criptato, lo salva temporaneamente in un percorso leggibile e
     * restituisce l'URI per permetterne l'apertura con un'applicazione esterna.
     *
     * @param encryptedFilePath Il percorso relativo del file criptato.
     * @param originalFileName Il nome originale del file, usato per il file temporaneo.
     * @return L'URI del file decifrato temporaneo, utilizzabile da FileProvider.
     * @throws IOException Se il file criptato non viene trovato o si verifica un errore di I/O.
     * @throws GeneralSecurityException Se si verifica un errore durante la decrittografia.
     */
    public Uri decryptAndOpenFile(String encryptedFilePath, String originalFileName) throws IOException, GeneralSecurityException {
        // Costruisce il percorso completo del file criptato
        File encryptedFile = new File(context.getFilesDir(), encryptedFilePath);

        if (!encryptedFile.exists()) {
            throw new IOException("File criptato non trovato: " + encryptedFilePath);
        }

        // Crea un'istanza di EncryptedFile.Builder per la decrittografia.
        EncryptedFile encryptedFileReader = new EncryptedFile.Builder(
                encryptedFile,
                context,
                masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        // Crea la directory per i file temporanei nella cache dell'app se non esiste
        File tempDir = new File(context.getCacheDir(), TEMP_FILES_DIR);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        // Genera un nome univoco per il file temporaneo decifrato.
        File tempDecryptedFile = new File(tempDir, "temp_" + UUID.randomUUID().toString() + "_" + originalFileName);
        // Elimina qualsiasi file temporaneo precedente con lo stesso nome per evitare conflitti.
        if (tempDecryptedFile.exists()) {
            tempDecryptedFile.delete();
        }

        // Apre un InputStream criptato e un FileOutputStream per il file temporaneo.
        try (FileInputStream encryptedInputStream = encryptedFileReader.openFileInput();
             FileOutputStream decryptedOutputStream = new FileOutputStream(tempDecryptedFile)) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = encryptedInputStream.read(buffer)) != -1) {
                decryptedOutputStream.write(buffer, 0, bytesRead); // Scrive i dati decifrati.
            }
            decryptedOutputStream.flush();// Assicura che tutti i dati siano scritti.
        }

        Log.d(TAG, "File decifrato temporaneamente in: " + tempDecryptedFile.getAbsolutePath());


        // Restituisce l'URI del file temporaneo utilizzando FileProvider, necessario per aprire file esterni.
        // Il fileprovider deve essere configurato nel manifest dell'app.
        return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileprovider", tempDecryptedFile);
    }

    /**
     * Elimina un file criptato dal filesystem.
     *
     * @param encryptedFilePath Il percorso relativo del file criptato da eliminare.
     * @return true se il file è stato eliminato con successo, false altrimenti.
     */
    public boolean deleteEncryptedFile(String encryptedFilePath) {
        File file = new File(context.getFilesDir(), encryptedFilePath);// Costruisce il percorso completo.
        if (file.exists()) {
            boolean deleted = file.delete();// Tenta di eliminare il file.
            if (deleted) {
                Log.d(TAG, "File criptato eliminato con successo: " + encryptedFilePath);
            } else {
                Log.e(TAG, "Impossibile eliminare il file criptato: " + encryptedFilePath);
            }
            return deleted;
        }
        Log.d(TAG, "Il file criptato non esiste: " + encryptedFilePath);
        return false;
    }

    /**
     * Pulisce tutti i file temporanei decifrati dalla directory della cache dell'applicazione.
     * Questa operazione viene eseguita su un thread separato per non bloccare l'UI.
     */
    public void cleanTempFiles() {
        // Esegue la pulizia in un thread dell'executor per operazioni I/O.
        fileIOExecutor.execute(() -> {
            try {
                File tempDir = new File(context.getCacheDir(), TEMP_FILES_DIR);
                if (tempDir.exists() && tempDir.isDirectory()) {
                    File[] files = tempDir.listFiles();// Ottiene la lista dei file nella directory temporanea.
                    if (files != null) {
                        for (File file : files) {
                            if (file.delete()) {
                                Log.d(TAG, "File temporaneo eliminato: " + file.getName());
                            } else {
                                Log.e(TAG, "Impossibile eliminare il file temporaneo: " + file.getName());
                            }
                        }
                    }
                    // Tenta di eliminare anche la directory temporanea se è vuota dopo aver eliminato i file.
                    if (tempDir.delete()) {
                        Log.d(TAG, "Directory temporanea pulita.");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Errore nella pulizia dei file temporanei: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Ottiene il nome del file da un dato URI.
     * Gestisce sia URI di tipo "content" che URI di tipo "file".
     *
     * @param uri L'URI del file.
     * @return Il nome del file.
     */
    public String getFileName(Uri uri) {
        String result = null;
        // Se l'URI è di tipo "content", usa ContentResolver per ottenere il DISPLAY_NAME.
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        // Se il nome non è stato trovato o l'URI non è "content", estrai il nome dal percorso.
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    /**
     * Ottiene il MIME type di un file da un dato URI.
     * Utile per determinare il tipo di contenuto del file (es. image/jpeg, application/pdf).
     *
     * @param uri L'URI del file.
     * @return Il MIME type del file, o null se non può essere determinato.
     */
    public String getMimeType(Uri uri) {
        String mimeType = null;
        // Se l'URI è di tipo "content", usa ContentResolver per ottenere il tipo.
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            mimeType = context.getContentResolver().getType(uri);
        } else {
            // Per gli URI di tipo "file", estrai l'estensione e usa MimeTypeMap.
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        return mimeType;
    }

    /**
     * Chiude l'executor per le operazioni di I/O sui file.
     * È importante chiamare questo metodo quando l'applicazione non necessita più del FileManager
     * (es. alla distruzione dell'Application) per rilasciare le risorse e terminare i thread.
     */
    public void shutdown() {
        if (fileIOExecutor != null && !fileIOExecutor.isShutdown()) {
            fileIOExecutor.shutdown();
        }
    }
}