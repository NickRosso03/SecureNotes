package com.example.securenotesapp.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Rappresenta un elemento file archiviato nell'applicazione SecureNotes.
 * Questa classe è un'entità di Room, che mappa una tabella nel database locale.
 * Contiene metadati sul file, come il nome originale, il tipo MIME, il percorso
 * crittografato sul filesystem e la sua dimensione, oltre a un timestamp
 * per l'ordinamento.
 */
@Entity(tableName = "file_items")
public class FileItem {
    /**
     * L'ID univoco del file nel database. È una chiave primaria con autogenerazione.
     */
    @PrimaryKey(autoGenerate = true)
    private int id;
    /**
     * Il nome originale del file prima della crittografia e dell'archiviazione.
     */
    private String originalFileName;
    /**
     * Il tipo MIME del file (es. "image/jpeg", "application/pdf").
     * Utile per determinare come aprire il file dopo la decrittografia.
     */
    private String mimeType;
    /**
     * Il percorso completo del file crittografato sul filesystem interno dell'app.
     */
    private String encryptedFilePath;
    private long fileSize; // Campo per la dimensione del file
    /**
     * Il timestamp che indica quando il file è stato aggiunto o modificato l'ultima volta.
     * Utilizzato per ordinare gli elementi della lista.
     */
    private long timestamp;
    /**
     * Costruttore per creare un nuovo oggetto FileItem.
     *
     * @param originalFileName Il nome originale del file.
     * @param mimeType Il tipo MIME del file.
     * @param encryptedFilePath Il percorso del file crittografato.
     * @param fileSize La dimensione del file in byte.
     * @param timestamp Il timestamp di creazione/ultima modifica del file.
     */

    public FileItem(String originalFileName, String mimeType, String encryptedFilePath, long fileSize, long timestamp) {
        this.originalFileName = originalFileName;
        this.mimeType = mimeType;
        this.encryptedFilePath = encryptedFilePath;
        this.fileSize = fileSize;
        this.timestamp = timestamp;
    }

    // --- Getter e Setter ---
    public int getId() {
        return id;
    }
    /**
     * Restituisce l'ID univoco del file.
     * @return L'ID del file.
     */

    public void setId(int id) {
        this.id = id;
    }
    /**
     * Restituisce il nome originale del file.
     * @return Il nome del file.
     */
    public String getOriginalFileName() {
        return originalFileName;
    }
    /**
     * Imposta il nome originale del file.
     * @param originalFileName Il nome del file da impostare.
     */
    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }
    /**
     * Restituisce il tipo MIME del file.
     * @return Il tipo MIME.
     */

    public String getMimeType() {
        return mimeType;
    }
    /**
     * Imposta il tipo MIME del file.
     * @param mimeType Il tipo MIME da impostare.
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    /**
     * Restituisce il percorso completo del file crittografato.
     * @return Il percorso del file crittografato.
     */

    public String getEncryptedFilePath() {
        return encryptedFilePath;
    }
    /**
     * Imposta il percorso completo del file crittografato.
     * @param encryptedFilePath Il percorso del file crittografato da impostare.
     */
    public void setEncryptedFilePath(String encryptedFilePath) {
        this.encryptedFilePath = encryptedFilePath;
    }
    /**
     * Restituisce la dimensione del file in byte.
     * @return La dimensione del file.
     */
    public long getFileSize() {
        return fileSize;
    }
    /**
     * Imposta la dimensione del file in byte.
     * @param fileSize La dimensione del file da impostare.
     */
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    /**
     * Restituisce il timestamp di creazione/ultima modifica del file.
     * @return Il timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }
    /**
     * Imposta il timestamp di creazione/ultima modifica del file.
     * @param timestamp Il timestamp da impostare.
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}