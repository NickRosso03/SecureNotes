package com.example.securenotesapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper; // Importante per lo swipe-to-delete

import com.example.securenotesapp.AddEditNoteActivity;
import com.example.securenotesapp.R;
import com.example.securenotesapp.adapter.NoteAdapter;
import com.example.securenotesapp.model.Note;
import com.example.securenotesapp.viewmodel.NoteViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

/**
 * Fragment responsabile della visualizzazione di un elenco di note e
 * della gestione delle operazioni CRUD (Creazione, Lettura, Aggiornamento, Eliminazione)
 * relative alle note. Utilizza un {@link RecyclerView} per visualizzare le note
 * e interagisce con {@link NoteViewModel} per la gestione dei dati persistenti.
 * Permette di aggiungere nuove note, modificarle esistenti tramite click e
 * eliminarle con un'azione di swipe.
 */

public class NotesFragment extends Fragment {

    public static final int ADD_NOTE_REQUEST = 1;
    public static final int EDIT_NOTE_REQUEST = 2;

    private NoteViewModel noteViewModel;
    private NoteAdapter adapter;
    /**
     * Chiamato per creare e restituire la gerarchia di View associata al Fragment.
     * Questo è il punto in cui si infla il layout e si inizializzano i componenti della UI.
     *
     * @param inflater L'LayoutInflater che può essere usato per creare istanze di View
     * da file XML layout.
     * @param container Se non nullo, questo è il ViewGroup genitore a cui la UI del Fragment
     * dovrebbe essere allegata.
     * @param savedInstanceState Se non nullo, questo Fragment sta venendo ricreato da un
     * precedente stato salvato.
     * @return La View radice del layout del Fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Gonfia il layout per questo frammento
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        FloatingActionButton buttonAddNote = view.findViewById(R.id.button_add_note);
        buttonAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditNoteActivity.class);
            startActivityForResult(intent, ADD_NOTE_REQUEST);
        });

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_notes);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        adapter = new NoteAdapter();
        recyclerView.setAdapter(adapter);

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        noteViewModel.getAllNotes().observe(getViewLifecycleOwner(), notes -> {
            // Aggiorna la UI quando i dati delle note cambiano
            adapter.setNotes(notes);
        });

        // Imposta il listener per i click sugli elementi della lista
        adapter.setOnItemClickListener(note -> {
            Intent intent = new Intent(getActivity(), AddEditNoteActivity.class);
            intent.putExtra(AddEditNoteActivity.EXTRA_NOTE_ID, note.getId());
            intent.putExtra(AddEditNoteActivity.EXTRA_NOTE_TITLE, note.getTitle());
            intent.putExtra(AddEditNoteActivity.EXTRA_NOTE_CONTENT, note.getContent());
            startActivityForResult(intent, EDIT_NOTE_REQUEST);
        });

        // Implementazione dello swipe-to-delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // Non supportiamo il drag & drop
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Quando un elemento viene swipato, lo si elimina dal database
                Note deletedNote = adapter.getNoteAt(viewHolder.getAdapterPosition());
                noteViewModel.delete(deletedNote);
                Toast.makeText(getContext(), "Nota eliminata", Toast.LENGTH_SHORT).show();

                // Snackbar con opzione UNDO
                Snackbar.make(recyclerView, "Nota eliminata", Snackbar.LENGTH_LONG)
                        .setAction("Annulla", v -> {
                            noteViewModel.insert(deletedNote); // Re-inserisci la nota
                            Toast.makeText(getContext(), "Eliminazione annullata", Toast.LENGTH_SHORT).show();
                        })
                        .show();
            }
        }).attachToRecyclerView(recyclerView);

        return view;
    }

    /**
     * Chiamato quando un'attività viene completata e restituisce un risultato. Questo metodo è qui per completezza, ma le operazioni
     * di inserimento/aggiornamento sono già gestite dal ViewModel e dalle LiveData,
     * quindi i cambiamenti nella UI avvengono automaticamente.
     *
     * @param requestCode Il codice di richiesta originale passato a startActivityForResult().
     * @param resultCode Il codice di risultato restituito dall'attività secondaria.
     * @param data Un Intent, che può restituire dati di risultato all'attività genitore.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Questa parte è gestita dalla AddEditNoteActivity in sé (salva/aggiorna tramite ViewModel)
        // Non è strettamente necessario gestire qui i risultati INSERT/UPDATE perché la LiveData
        // osserverà i cambiamenti e aggiornerà automaticamente il RecyclerView.
        // Tuttavia, porebbe mostrare messaggi di Toast specifici se l'operazione ha avuto successo.

        if (requestCode == ADD_NOTE_REQUEST) {
            if (resultCode == getActivity().RESULT_OK) {
                // Nota: AddEditNoteActivity non restituisce dati dell'oggetto Note, ma solo un risultato OK.
                // Il ViewModel si occupa del salvataggio.
                // Toast.makeText(getContext(), "Nota aggiunta/modificata", Toast.LENGTH_SHORT).show();
            } else {
                // Toast.makeText(getContext(), "Operazione nota annullata", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == EDIT_NOTE_REQUEST) {
            if (resultCode == getActivity().RESULT_OK) {
                // Toast.makeText(getContext(), "Nota modificata con successo", Toast.LENGTH_SHORT).show();
            } else {
                // Toast.makeText(getContext(), "Modifica nota annullata", Toast.LENGTH_SHORT).show();
            }
        }
    }
}