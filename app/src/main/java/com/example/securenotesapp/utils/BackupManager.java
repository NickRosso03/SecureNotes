package com.example.securenotesapp.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.securenotesapp.dao.FileDao;
import com.example.securenotesapp.dao.NoteDao;
import com.example.securenotesapp.database.NoteDatabase;
import com.example.securenotesapp.model.FileItem;
import com.example.securenotesapp.model.Note;
import com.example.securenotesapp.KeyManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
/**
 * Gestisce le operazioni di backup e ripristino dei dati dell'applicazione (note e metadati dei file)
 * e dei file criptati stessi.
 * Questo gestore crea un file ZIP che contiene i dati del database (note e metadati dei file)
 * serializzati in JSON e i file binari criptati originali. L'intero file ZIP
 * viene poi criptato utilizzando una password fornita dall'utente con AES/GCM.
 * Ciò aggiunge un ulteriore livello di sicurezza per il backup, indipendente dalla
 * passphrase interna del database.
 */
public class BackupManager {

    private static final String TAG = "BackupManager"; // Tag per i log, utile per identificare i messaggi di log di questa classe.
    private static final String ALGORITHM = "AES";// Algoritmo di crittografia utilizzato, Advanced Encryption Standard.
    private static final String TRANSFORMATION = "AES/GCM/NoPadding"; // Modalità di crittografia AES con GCM
    private static final int BUFFER_SIZE = 8192;// Dimensione del buffer in byte per le operazioni di I/O, 8KB per migliorare le prestazioni.

    private final Context context; // Contesto dell'applicazione, necessario per accedere a risorse e servizi di sistema.
    private final NoteDao noteDao; // Data Access Object per le note, permette di interagire con la tabella delle note nel database.
    private final FileDao fileDao; // Data Access Object per i metadati dei file, permette di interagire con la tabella dei file nel database.
    private final KeyManager keyManager; // Gestore delle chiavi di crittografia, utilizzato per ottenere le chiavi necessarie e i percorsi dei file criptati.
    /**
     * Costruttore per il BackupManager.
     * Inizializza i DAO per accedere al database e il KeyManager per la gestione delle chiavi
     * e dei percorsi dei file criptati.
     *
     * @param context Il contesto dell'applicazione.
     * @throws IllegalStateException Se la passphrase del database non è disponibile,
     * indicando un problema nell'inizializzazione dell'app.
     */
    public BackupManager(Context context) {
        this.context = context;
        this.keyManager = new KeyManager(context); // Inizializza il KeyManager
        byte[] dbPassphrase = keyManager.retrievePassphrase(); // Recupera la passphrase del DB
        if (dbPassphrase == null) {
            Log.e(TAG, "Database passphrase not found! Cannot initialize database for backup/restore.");
            throw new IllegalStateException("Database passphrase not found. App must be initialized first.");
        }
        // Ottiene l'istanza del database Room, passando la passphrase per SQLCipher.
        NoteDatabase db = NoteDatabase.getDatabase(context, dbPassphrase); // Ora passiamo la passphrase
        this.noteDao = db.noteDao();// Ottiene l'interfaccia DAO per le note.
        this.fileDao = db.fileDao();// Ottiene l'interfaccia DAO per i file.
    }

    /**
     * Esegue il backup delle note e dei metadati dei file in un file ZIP crittografato.
     * Il backup include i dati del database (note e metadati dei file) serializzati in JSON
     * e i contenuti binari dei file criptati originali. L'intero file ZIP viene poi
     * [cite_start]criptato usando una password fornita dall'utente con AES/GCM.
     *
     * @param outputUri URI di destinazione dove salvare il file di backup (es. un file selezionato dall'utente).
     * @param password Password fornita dall'utente per la crittografia del file di backup ZIP.
     * @param progressCallback Callback per aggiornare l'interfaccia utente sull'avanzamento dell'operazione.
     * @return true se il backup ha successo, false altrimenti.
     */
    public boolean exportBackup(Uri outputUri, String password, ProgressCallback progressCallback) {
        Log.d(TAG, "Starting export backup to: " + outputUri.getPath());
        try {
            // Genera la chiave AES dalla password fornita dall'utente.
            SecretKeySpec secretKey = keyManager.generateAesKeyFromPassword(password);
            // Genera un Initialization Vector (IV) casuale per la modalità GCM.
            byte[] iv = keyManager.generateIv();
            // Apre un OutputStream per scrivere sul file di destinazione specificato dall'URI.
            OutputStream os = context.getContentResolver().openOutputStream(outputUri);
            if (os == null) {
                Log.e(TAG, "Failed to open output stream for URI: " + outputUri);
                return false;
            }
            // Inizializza il Cipher per la crittografia in modalità AES/GCM.
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

            // Scrive IV all'inizio del file per la decrittografia
            os.write(iv);

            try (CipherOutputStream cos = new CipherOutputStream(os, cipher);// Stream che cripta i dati prima di scriverli.
                 ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(cos))) {// Stream che zippa i dati e li scrive tramite CipherOutputStream.

                // 1. Salva le note nel file ZIP.
                Log.d(TAG, "Exporting notes...");
                // Recupera tutte le note dal database in modo sincrono.
                List<Note> notes = noteDao.getAllNotesSync();
                // Converte la lista di note in una stringa JSON.
                String notesJson = new Gson().toJson(notes);
                // Crea una nuova entry nello ZIP per il file JSON delle note.
                zos.putNextEntry(new ZipEntry("notes.json"));
                // Scrive i byte della stringa JSON nell'entry dello ZIP.
                zos.write(notesJson.getBytes());
                // Chiude l'entry corrente nello ZIP.
                zos.closeEntry();
                // Aggiorna la UI con l'avanzamento.
                progressCallback.onProgressUpdate(25, "Salvataggio note...");
                Log.d(TAG, "Notes exported. Count: " + notes.size());

                // 2. Salva i metadati dei file (FileItem) nel file ZIP.
                Log.d(TAG, "Exporting file items metadata...");
                // Recupera tutti i metadati dei file dal database in modo sincrono.
                List<FileItem> fileItems = fileDao.getAllFileItemsSync();
                // Converte la lista di metadati dei file in una stringa JSON.
                String fileItemsJson = new Gson().toJson(fileItems);
                // Crea una nuova entry nello ZIP per il file JSON dei metadati.
                zos.putNextEntry(new ZipEntry("file_items.json"));
                // Scrive i byte della stringa JSON nell'entry dello ZIP.
                zos.write(fileItemsJson.getBytes());
                // Chiude l'entry corrente nello ZIP.
                zos.closeEntry();
                // Aggiorna la UI con l'avanzamento.
                progressCallback.onProgressUpdate(50, "Salvataggio metadati file...");
                Log.d(TAG, "File items metadata exported. Count: " + fileItems.size());


                // 3. Salva i contenuti binari dei file criptati nel file ZIP.
                Log.d(TAG, "Exporting encrypted file contents...");
                for (int i = 0; i < fileItems.size(); i++) {
                    FileItem item = fileItems.get(i);
                    // Ottiene il percorso del file criptato locale.
                    File encryptedFile = keyManager.getEncryptedFile(context, item.getOriginalFileName());
                    // Controlla se il file criptato esiste.
                    if (encryptedFile != null && encryptedFile.exists()) {
                        Log.d(TAG, "Adding encrypted file: " + item.getOriginalFileName());
                        // Aggiunge una nuova entry nello ZIP, utilizzando una sottocartella "files".
                        zos.putNextEntry(new ZipEntry("files/" + item.getOriginalFileName()));
                        // Apre un FileInputStream per leggere il contenuto del file criptato.
                        try (FileInputStream fis = new FileInputStream(encryptedFile);
                             BufferedInputStream bis = new BufferedInputStream(fis)) {
                            byte[] buffer = new byte[BUFFER_SIZE];
                            int count;
                            // Legge dal file criptato e scrive nell'entry dello ZIP.
                            while ((count = bis.read(buffer)) != -1) {
                                zos.write(buffer, 0, count);
                            }
                        }
                        // Chiude l'entry corrente nello ZIP.
                        zos.closeEntry();
                    } else {
                        Log.w(TAG, "Encrypted file not found for: " + item.getOriginalFileName());
                    }
                    // Calcola e aggiorna l'avanzamento basato sul numero di file elaborati.
                    int progress = 50 + (int) ((i + 1) * 50.0 / fileItems.size());
                    progressCallback.onProgressUpdate(progress, "Salvataggio file: " + item.getOriginalFileName());
                }
                // Aggiorna la UI al completamento.
                progressCallback.onProgressUpdate(100, "Backup completato!");
                Log.d(TAG, "Backup completed successfully.");
                return true;

            } catch (IOException e) {
                // Gestisce le eccezioni di I/O durante il backup.
                Log.e(TAG, "IO exception during backup: " + e.getMessage(), e);
                progressCallback.onProgressUpdate(-1, "Errore I/O durante il backup.");
                return false;
            } finally {
                // Assicura che l'OutputStream principale venga chiuso.
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing output stream: " + e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            // Gestisce eccezioni generiche durante la fase di setup del backup.
            Log.e(TAG, "Error during export backup setup: " + e.getMessage(), e);
            progressCallback.onProgressUpdate(-1, "Errore generico durante l'export.");
            return false;
        }
    }

    /**
     * Esegue il ripristino delle note e dei file da un file ZIP crittografato.
     * @param inputUri URI del file di backup da ripristinare.
     * @param password Password per la decrittografia del backup.
     * @param progressCallback Callback per aggiornare l'avanzamento.
     * @return true se il ripristino ha successo, false altrimenti.
     */
    public boolean importBackup(Uri inputUri, String password, ProgressCallback progressCallback) {
        Log.d(TAG, "Starting import backup from: " + inputUri.getPath());
        try {
            // Apre un InputStream per leggere dal file di backup specificato dall'URI.
            InputStream is = context.getContentResolver().openInputStream(inputUri);
            if (is == null) {
                Log.e(TAG, "Failed to open input stream for URI: " + inputUri);
                return false;
            }

            // Legge l'IV (Initialization Vector) dall'inizio del file.
            byte[] iv = new byte[12]; // GCM IV è tipicamente 12 byte
            int bytesRead = is.read(iv);
            if (bytesRead != iv.length) {
                Log.e(TAG, "Could not read IV from backup file.");
                return false;
            }

            // Ottiene la chiave AES dalla password fornita dall'utente.
            SecretKeySpec secretKey = keyManager.generateAesKeyFromPassword(password);

            // Inizializza il Cipher per la decrittografia in modalità AES/GCM.
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            // Usa try-with-resources per assicurare la chiusura automatica degli stream.
            try (CipherInputStream cis = new CipherInputStream(is, cipher);// Stream che decrittografa i dati.
                 ZipInputStream zis = new ZipInputStream(new BufferedInputStream(cis))) {// Stream che decomprime i dati decifrati.

                ZipEntry zipEntry;// Rappresenta una singola entry (file o directory) all'interno del file ZIP.
                List<Note> notesToRestore = null;// Lista temporanea per le note da ripristinare.
                List<FileItem> fileItemsToRestore = null; // Lista temporanea per i metadati dei file da ripristinare.

                // Itera su ogni entry all'interno del file ZIP.
                while ((zipEntry = zis.getNextEntry()) != null) {
                    Log.d(TAG, "Processing zip entry: " + zipEntry.getName());

                    if (zipEntry.getName().equals("notes.json")) {
                        // Se l'entry è "notes.json", legge il contenuto e lo deserializza in una lista di Note.
                        Log.d(TAG, "Found notes.json");
                        String notesJson = readZipEntryContent(zis);// Legge il contenuto JSON dall'entry ZIP.
                        Type noteListType = new TypeToken<List<Note>>(){}.getType();// Definisce il tipo di lista per la deserializzazione JSON.
                        notesToRestore = new Gson().fromJson(notesJson, noteListType);// Deserializza la stringa JSON in una lista di oggetti Note.
                        progressCallback.onProgressUpdate(25, "Caricamento note...");
                    } else if (zipEntry.getName().equals("file_items.json")) {
                        // Se l'entry è "file_items.json", legge il contenuto e lo deserializza in una lista di FileItem.
                        Log.d(TAG, "Found file_items.json");
                        String fileItemsJson = readZipEntryContent(zis);// Legge il contenuto JSON dall'entry ZIP.
                        Type fileItemListType = new TypeToken<List<FileItem>>(){}.getType();// Definisce il tipo di lista per la deserializzazione JSON.
                        fileItemsToRestore = new Gson().fromJson(fileItemsJson, fileItemListType);// Deserializza la stringa JSON in una lista di oggetti FileItem.
                        progressCallback.onProgressUpdate(50, "Caricamento metadati file...");
                    } else if (zipEntry.getName().startsWith("files/")) {
                        // Se l'entry inizia con "files/", si tratta di un contenuto di file criptato.
                        // Ripristina i contenuti dei file criptati nella loro posizione locale.
                        String filename = zipEntry.getName().substring("files/".length());
                        Log.d(TAG, "Found encrypted file content: " + filename);

                        // Ottiene l'oggetto File che rappresenta il percorso dove il file criptato dovrebbe essere ripristinato.
                        File restoredEncryptedFile = keyManager.getEncryptedFile(context, filename);
                        if (restoredEncryptedFile != null) {
                            // Apre un FileOutputStream per scrivere il contenuto nel file locale.
                            try (FileOutputStream fos = new FileOutputStream(restoredEncryptedFile);
                                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                                byte[] buffer = new byte[BUFFER_SIZE];
                                int count;
                                // Legge dal ZipInputStream e scrive nel FileOutputStream.
                                while ((count = zis.read(buffer)) != -1) {
                                    bos.write(buffer, 0, count);
                                }
                                bos.flush();// Assicura che tutti i dati siano scritti sul disco.
                                Log.d(TAG, "Restored encrypted file content: " + filename);
                            }
                        } else {
                            Log.w(TAG, "Could not get encrypted file path for restore: " + filename);
                        }
                    }
                    zis.closeEntry();// Chiude l'entry corrente prima di passare alla successiva.
                }
                // Una volta estratte tutte le entry, inserisce i dati nel database.
                if (notesToRestore != null && !notesToRestore.isEmpty()) {
                    Log.d(TAG, "Restoring notes to database. Count: " + notesToRestore.size());
                    noteDao.insertAll(notesToRestore); // Inserisce tutte le note ripristinate nel database
                    progressCallback.onProgressUpdate(75, "Ripristino note nel database...");
                }
                if (fileItemsToRestore != null && !fileItemsToRestore.isEmpty()) {
                    Log.d(TAG, "Restoring file items to database. Count: " + fileItemsToRestore.size());
                    fileDao.insertAll(fileItemsToRestore); // Inserisce tutti i metadati dei file ripristinati nel database.
                    progressCallback.onProgressUpdate(90, "Ripristino metadati file nel database...");
                }
                // Aggiorna la UI al completamento.
                progressCallback.onProgressUpdate(100, "Ripristino completato!");
                Log.d(TAG, "Restore completed successfully.");
                return true;

            } catch (IOException e) {
                // Gestisce le eccezioni di I/O durante il ripristino.
                Log.e(TAG, "IO exception during restore: " + e.getMessage(), e);
                progressCallback.onProgressUpdate(-1, "Errore I/O durante il ripristino.");
                return false;
            } finally {
                // Assicura che l'InputStream principale venga chiuso.
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing input stream: " + e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            // Gestisce eccezioni generiche durante la fase di setup del ripristino.
            Log.e(TAG, "Error during import backup setup: " + e.getMessage(), e);
            progressCallback.onProgressUpdate(-1, "Errore generico durante l'import.");
            return false;
        }
    }
    /**
     * Legge il contenuto di una singola entry da un ZipInputStream e lo restituisce come stringa.
     * Questa funzione è utile per leggere i file JSON (notes.json, file_items.json) all'interno del backup ZIP.
     *
     * @param zis Lo ZipInputStream da cui leggere l'entry corrente.
     * @return Il contenuto dell'entry come stringa.
     * @throws IOException Se si verifica un errore di I/O durante la lettura.
     */
    private String readZipEntryContent(ZipInputStream zis) throws IOException {
        StringBuilder builder = new StringBuilder();// Utilizza StringBuilder per costruire la stringa in modo efficiente.
        byte[] buffer = new byte[BUFFER_SIZE];// Buffer per la lettura dei dati.
        int count;// Numero di byte letti.
        // Legge a blocchi dall'InputStream fino alla fine dell'entry.
        while ((count = zis.read(buffer)) != -1) {
            builder.append(new String(buffer, 0, count));// Appende i byte letti al StringBuilder, convertendoli in stringa.
        }
        return builder.toString();// Restituisce il contenuto completo come stringa.
    }

    /**
     * Interfaccia di callback per aggiornare l'avanzamento delle operazioni di backup/ripristino.
     * Questo permette all'interfaccia utente (es. una ProgressBar o un TextView) di mostrare lo stato corrente.
     */
    public interface ProgressCallback {
        /**
         * Chiamato per aggiornare l'avanzamento dell'operazione.
         * @param progress Un valore intero che indica l'avanzamento (es. 0-100%).
         * @param message Un messaggio descrittivo sullo stato attuale dell'operazione.
         */
        void onProgressUpdate(int progress, String message);
    }
}