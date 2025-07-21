package com.example.securenotesapp.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.securenotesapp.model.FileItem;

import java.util.List;
/**
 * Interfaccia Data Access Object (DAO) per l'entità {@link com.example.securenotesapp.model.FileItem}.
 * Fornisce i metodi per interagire con la tabella 'file_items' nel database Room.
 * Le annotazioni di Room gestiscono l'implementazione concreta di questi metodi.
 */
@Dao
public interface FileDao {
    /**
     * Inserisce un nuovo oggetto {@link com.example.securenotesapp.model.FileItem} nel database.
     *
     * @param fileItem L'oggetto FileItem da inserire.
     */
    @Insert
    void insert(FileItem fileItem);
    /**
     * Aggiorna un oggetto {@link com.example.securenotesapp.model.FileItem} esistente nel database.
     * La nota viene identificata tramite il suo ID.
     *
     * @param fileItem L'oggetto FileItem da aggiornare.
     */
    @Update
    void update(FileItem fileItem);
    /**
     * Elimina un oggetto {@link com.example.securenotesapp.model.FileItem} dal database.
     * L'elemento viene identificato tramite il suo ID.
     *
     * @param fileItem L'oggetto FileItem da eliminare.
     */
    @Delete
    void delete(FileItem fileItem);
    /**
     * Recupera tutti gli oggetti {@link com.example.securenotesapp.model.FileItem} dal database,
     * ordinati per timestamp in ordine decrescente (dal più recente al meno recente).
     *
     * @return Un oggetto {@link androidx.lifecycle.LiveData} contenente una lista di tutti gli elementi FileItem.
     * LiveData è osservabile e si aggiorna automaticamente quando i dati nel database cambiano.
     */
    @Query("SELECT * FROM file_items ORDER BY timestamp DESC")
    LiveData<List<FileItem>> getAllFileItems();
    /**
     * Recupera un singolo oggetto {@link com.example.securenotesapp.model.FileItem} dal database
     * tramite il suo ID.
     *
     * @param fileItemId L'ID dell'elemento FileItem da recuperare.
     * @return Un oggetto {@link androidx.lifecycle.LiveData} contenente l'elemento FileItem corrispondente all'ID.
     */
    @Query("SELECT * FROM file_items WHERE id = :fileItemId")
    LiveData<FileItem> getFileItemById(int fileItemId);
    /**
     * Recupera tutti gli oggetti {@link com.example.securenotesapp.model.FileItem} dal database,
     * ordinati per timestamp in ordine decrescente (dal più recente al meno recente), in modo sincrono.
     * Questo metodo è utile per operazioni che non richiedono osservazione continua, come il backup.
     *
     * @return Una lista di tutti gli oggetti FileItem.
     */
    @Query("SELECT * FROM file_items ORDER BY timestamp DESC")
    List<FileItem> getAllFileItemsSync(); // Metodo sincrono per il backup
    /**
     * Inserisce una lista di oggetti {@link com.example.securenotesapp.model.FileItem} nel database.
     * Se un elemento con lo stesso ID esiste già, viene sostituito.
     * Questo è utile per le operazioni di ripristino del backup.
     *
     * @param fileItems La lista di oggetti FileItem da inserire.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Sostituisce se c'è un conflitto sull'ID
    void insertAll(List<FileItem> fileItems);
}