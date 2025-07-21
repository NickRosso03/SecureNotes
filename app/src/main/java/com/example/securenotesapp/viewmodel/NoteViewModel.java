package com.example.securenotesapp.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.securenotesapp.model.Note;
import java.util.List;

/**
 * {@code NoteViewModel} è un ViewModel che gestisce i dati delle note per la UI.
 * Agisce da intermediario tra il Repository e la UI, fornendo dati reattivi tramite LiveData.
 * Questo ViewModel estende {@link AndroidViewModel} per avere accesso al contesto dell'applicazione.
 */
public class NoteViewModel extends AndroidViewModel {
    private NoteRepository repository;// Dichiarazione del repository per l'accesso ai dati.
    private LiveData<List<Note>> allNotes;// LiveData che conterrà tutte le note, aggiornandosi automaticamente.

    /**
     * Costruttore per il {@code NoteViewModel}.
     * Inizializza il {@link NoteRepository} e recupera tutte le note dal repository.
     *
     * @param application L'istanza dell'applicazione, necessaria per AndroidViewModel.
     */
    public NoteViewModel(Application application) {
        super(application);
        // Inizializza il NoteRepository passando il contesto dell'applicazione.
        repository = new NoteRepository(application);
        // Ottiene il LiveData di tutte le note dal repository.
        allNotes = repository.getAllNotes();
    }

    /**
     * Restituisce un {@link LiveData} contenente una lista di tutte le note.
     * Questo LiveData si aggiornerà automaticamente quando i dati nel database cambiano.
     *
     * @return Un {@link LiveData} di una lista di {@link Note}.
     */
    public LiveData<List<Note>> getAllNotes() {
        return allNotes;
    }

    /**
     * Restituisce un {@link LiveData} di una singola nota, cercandola per ID.
     * Utile per visualizzare i dettagli di una nota specifica.
     *
     * @param id L'ID della nota da recuperare.
     * @return Un {@link LiveData} della {@link Note} corrispondente all'ID.
     */
    public LiveData<Note> getNoteById(int id) {
        return repository.getNoteById(id);
    }

    /**
     * Inserisce una nuova nota nel database.
     * L'operazione viene gestita dal repository, tipicamente su un thread separato.
     *
     * @param note La {@link Note} da inserire.
     */
    public void insert(Note note) {
        repository.insert(note);
    }

    /**
     * Aggiorna una nota esistente nel database.
     * L'operazione viene gestita dal repository, tipicamente su un thread separato.
     *
     * @param note La {@link Note} da aggiornare.
     */

    public void update(Note note) {
        repository.update(note);
    }
    /**
     * Elimina una nota dal database.
     * L'operazione viene gestita dal repository, tipicamente su un thread separato.
     *
     * @param note La {@link Note} da eliminare.
     */

    public void delete(Note note) {
        repository.delete(note);
    }
}