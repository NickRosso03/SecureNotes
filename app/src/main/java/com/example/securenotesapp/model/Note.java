package com.example.securenotesapp.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
/**
 * Rappresenta una singola nota archiviata nell'applicazione SecureNotes.
 * Questa classe è un'entità di Room, che mappa una tabella nel database locale.
 * Contiene l'ID univoco della nota, il suo titolo, il contenuto testuale
 * e un timestamp per l'ordinamento.
 */
@Entity(tableName = "notes") // Definisce la classe come un'entità Room e il nome della tabella nel DB
public class Note {
    /**
     * L'ID univoco della nota nel database. È una chiave primaria con autogenerazione.
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "title") // Nome della colonna nel DB
    private String title;

    @ColumnInfo(name = "content") // Nome della colonna nel DB
    private String content;

    @ColumnInfo(name = "timestamp") // Per memorizzare la data/ora di creazione o ultima modifica
    private long timestamp;

    /**
     * Costruttore per creare un nuovo oggetto Note.
     *
     * @param title Il titolo della nota.
     * @param content Il contenuto testuale della nota.
     * @param timestamp Il timestamp di creazione/ultima modifica della nota.
     */
    public Note(String title, String content, long timestamp) {
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
    }

    // --- Getter e Setter --- Room richiede che tutti i campi dell'entità abbiano getter e setter pubblici

    /**
     * Restituisce l'ID univoco della nota.
     * @return L'ID della nota.
     */
    public int getId() {
        return id;
    }
    /**
     * Imposta l'ID univoco della nota. Questo è tipicamente gestito da Room per le nuove entità.
     * @param id L'ID da impostare.
     */
    public void setId(int id) {
        this.id = id;
    }
    /**
     * Restituisce il titolo della nota.
     * @return Il titolo della nota.
     */
    public String getTitle() {
        return title;
    }
    /**
     * Imposta il titolo della nota.
     * @param title Il titolo da impostare.
     */
    public void setTitle(String title) {
        this.title = title;
    }
    /**
     * Restituisce il contenuto testuale della nota.
     * @return Il contenuto della nota.
     */

    public String getContent() {
        return content;
    }
    /**
     * Imposta il contenuto testuale della nota.
     * @param content Il contenuto da impostare.
     */
    public void setContent(String content) {
        this.content = content;
    }
    /**
     * Restituisce il timestamp di creazione/ultima modifica della nota.
     * @return Il timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }
    /**
     * Imposta il timestamp di creazione/ultima modifica della nota.
     * @param timestamp Il timestamp da impostare.
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}