package com.example.securenotesapp.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.securenotesapp.model.Note;

import java.util.List;
/**
 * Interfaccia Data Access Object (DAO) per l'entità {@link com.example.securenotesapp.model.Note}.
 * Fornisce i metodi per interagire con la tabella 'notes' nel database Room.
 * Le annotazioni di Room gestiscono l'implementazione concreta di questi metodi.
 */
@Dao // Indica che questa interfaccia è un DAO di Room
public interface NoteDao {
    /**
     * Inserisce un nuovo oggetto {@link com.example.securenotesapp.model.Note} nel database.
     *
     * @param note L'oggetto Note da inserire.
     */
    @Insert // Annotazione per inserire una o più note nel DB
    void insert(Note note);
    /**
     * Aggiorna un oggetto {@link com.example.securenotesapp.model.Note} esistente nel database.
     * La nota viene identificata tramite il suo ID.
     *
     * @param note L'oggetto Note da aggiornare.
     */
    @Update // Annotazione per aggiornare una o più note esistenti nel DB
    void update(Note note);
    /**
     * Elimina un oggetto {@link com.example.securenotesapp.model.Note} dal database.
     * La nota viene identificata tramite il suo ID.
     *
     * @param note L'oggetto Note da eliminare.
     */

    @Delete // Annotazione per eliminare una o più note dal DB
    void delete(Note note);
    /**
     * Recupera tutti gli oggetti {@link com.example.securenotesapp.model.Note} dal database,
     * ordinati per timestamp in ordine decrescente (dal più recente al meno recente).
     *
     * @return Un oggetto {@link androidx.lifecycle.LiveData} contenente una lista di tutte le note.
     * LiveData è osservabile e si aggiorna automaticamente quando i dati nel database cambiano.
     */
    @Query("SELECT * FROM notes ORDER BY timestamp DESC") // Query personalizzata per ottenere tutte le note, ordinate dalla più recente
    LiveData<List<Note>> getAllNotes();

    /**
     * Recupera un singolo oggetto {@link com.example.securenotesapp.model.Note} dal database
     * tramite il suo ID.
     *
     * @param noteId L'ID della nota da recuperare.
     * @return Un oggetto {@link androidx.lifecycle.LiveData} contenente la nota corrispondente all'ID.
     */
    @Query("SELECT * FROM notes WHERE id = :noteId")
    LiveData<Note> getNoteById(int noteId);
    /**
     * Recupera tutti gli oggetti {@link com.example.securenotesapp.model.Note} dal database,
     * ordinati per timestamp in ordine decrescente (dal più recente al meno recente), in modo sincrono.
     * Questo metodo è utile per operazioni che non richiedono osservazione continua, come il backup.
     *
     * @return Una lista di tutti gli oggetti Note.
     */
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    List<Note> getAllNotesSync(); // Metodo sincrono per il backup
    /**
     * Inserisce una lista di oggetti {@link com.example.securenotesapp.model.Note} nel database.
     * Se una nota con lo stesso ID esiste già, viene sostituita.
     * Questo è utile per le operazioni di ripristino del backup.
     *
     * @param notes La lista di oggetti Note da inserire.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Sostituisce se c'è un conflitto sull'ID
    void insertAll(List<Note> notes);
}