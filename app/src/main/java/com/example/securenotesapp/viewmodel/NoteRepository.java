package com.example.securenotesapp.viewmodel;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.securenotesapp.dao.NoteDao;
import com.example.securenotesapp.database.NoteDatabase;
import com.example.securenotesapp.model.Note;
import com.example.securenotesapp.KeyManager; // Importa KeyManager

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository per la gestione dei dati delle note.
 * Questa classe astrae l'accesso ai dati dal resto dell'applicazione,
 * fornendo un'API pulita per interagire con il database Room.
 * Gestisce le operazioni sul database in un thread separato per evitare di bloccare
 * il thread principale dell'UI. Utilizza {@link LiveData} per fornire dati osservabili.
 */
public class NoteRepository {
    private NoteDao noteDao;// Data Access Object per le note, usato per interagire con il database.
    private LiveData<List<Note>> allNotes;// LiveData che contiene la lista di tutte le note.
    // Pool di thread per eseguire operazioni sul database in background.
    private ExecutorService databaseExecutor;

    /**
     * Costruttore per il NoteRepository.
     * Inizializza il database Room utilizzando la passphrase recuperata tramite {@link KeyManager}.
     * Le operazioni sul database vengono eseguite su un pool di thread separato.
     *
     * @param application L'istanza dell'applicazione, necessaria per l'inizializzazione del database.
     * @throws RuntimeException Se la passphrase del database non viene trovata o
     * se c'è un errore nell'inizializzazione del database.
     */
    public NoteRepository(Application application) {
        try {
            // Inizializza un pool di 4 thread per eseguire le operazioni sul database.
            databaseExecutor = Executors.newFixedThreadPool(4);

            // Ottiene la passphrase tramite KeyManager.
            KeyManager keyManager = new KeyManager(application); // Inizializza KeyManager
            byte[] passphrase = keyManager.retrievePassphrase();  // Recupera la passphrase esistente per il database criptato.

            if (passphrase == null) {
                // Questo caso non dovrebbe verificarsi se LoginActivity funziona correttamente,
                // ma è una buona pratica gestire l'assenza della passphrase.
                throw new IllegalStateException("Database passphrase not found. Ensure login process completed.");
            }

            // Inizializza il database Room (singleton), passando la passphrase per SQLCipher
            NoteDatabase database = NoteDatabase.getDatabase(application, passphrase);
            noteDao = database.noteDao();// Ottiene l'istanza del DAO per le note.
            allNotes = noteDao.getAllNotes();// Recupera tutte le note come LiveData

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Errore nell'inizializzazione del database o recupero passphrase", e);
        }
    }
    /**
     * Restituisce un {@link LiveData} contenente una lista di tutte le note.
     * Questo LiveData può essere osservato dall'interfaccia utente per ricevere
     * aggiornamenti automatici quando la lista delle note nel database cambia.
     *
     * @return Un LiveData con la lista di {@link Note}.
     */
    public LiveData<List<Note>> getAllNotes() {
        return allNotes;
    }
    /**
     * Restituisce un {@link LiveData} contenente una singola nota basata sul suo ID.
     * Utile per osservare i cambiamenti di una nota specifica.
     *
     * @param id L'ID della nota da recuperare.
     * @return Un LiveData con l'oggetto {@link Note} corrispondente all'ID.
     */
    public LiveData<Note> getNoteById(int id) {
        return noteDao.getNoteById(id);
    }
    /**
     * Inserisce una nuova nota nel database.
     * L'operazione viene eseguita in un thread separato gestito da {@code databaseExecutor}.
     *
     * @param note L'oggetto {@link Note} da inserire.
     */
    public void insert(Note note) {
        // Esegue l'operazione di inserimento sul database in un thread del pool.
        databaseExecutor.execute(() -> {
            noteDao.insert(note);
        });
    }
    /**
     * Aggiorna una nota esistente nel database.
     * L'operazione viene eseguita in un thread separato gestito da {@code databaseExecutor}.
     *
     * @param note L'oggetto {@link Note} da aggiornare.
     */
    public void update(Note note) {
        // Esegue l'operazione di aggiornamento sul database in un thread del pool.
        databaseExecutor.execute(() -> {
            noteDao.update(note);
        });
    }
    /**
     * Elimina una nota dal database.
     * L'operazione viene eseguita in un thread separato gestito da {@code databaseExecutor}.
     *
     * @param note L'oggetto {@link Note} da eliminare.
     */
    public void delete(Note note) {
        databaseExecutor.execute(() -> {
            noteDao.delete(note);
        });
    }
}