package com.example.securenotesapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.example.securenotesapp.model.Note;
import com.example.securenotesapp.viewmodel.NoteViewModel;
/**
 * Activity responsabile per l'aggiunta di nuove note o la modifica di note esistenti.
 * Questa Activity consente all'utente di inserire un titolo e un contenuto per una nota.
 * Utilizza un {@link NoteViewModel} per interagire con il database in modo asincrono.
 */

public class AddEditNoteActivity extends AppCompatActivity {
    // Costanti per le chiavi degli extra Intent, usate per passare dati tra Activity.
    public static final String EXTRA_NOTE_ID = "com.example.securenotesapp.EXTRA_NOTE_ID";
    public static final String EXTRA_NOTE_TITLE = "com.example.securenotesapp.EXTRA_NOTE_TITLE";
    public static final String EXTRA_NOTE_CONTENT = "com.example.securenotesapp.EXTRA_NOTE_CONTENT";

    private EditText editTextTitle;// Campo di testo per il titolo della nota.
    private EditText editTextContent;// Campo di testo per il contenuto della nota.

    private NoteViewModel noteViewModel;// ViewModel per gestire i dati relativi alle note.
    // Variabile per memorizzare l'ID della nota. -1 indica una nuova nota, altrimenti è l'ID di una nota esistente.
    private int noteId = -1;
    /**
     * Chiamato quando l'Activity viene creata per la prima volta.
     * Qui vengono inizializzati gli elementi dell'interfaccia utente e vengono gestiti
     * i dati passati tramite Intent per popolare i campi in caso di modifica di una nota esistente.
     * @param savedInstanceState Se l'Activity viene ricreata dopo essere stata terminata,
     * questo Bundle contiene i dati forniti .
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Imposta il layout per questa Activity.
        setContentView(R.layout.activity_add_edit_note);

        // Inizializza la Toolbar trovandola dal layout.
        Toolbar toolbar = findViewById(R.id.toolbar_add_edit_note);
        // Imposta la Toolbar come ActionBar predefinita per questa Activity.
        setSupportActionBar(toolbar);

        // Abilita il pulsante "Indietro" (freccia su) nella Toolbar.
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // Inizializza i riferimenti agli EditText dal layout.
        editTextTitle = findViewById(R.id.edit_text_note_title);
        editTextContent = findViewById(R.id.edit_text_note_content);

        // Inizializza il NoteViewModel utilizzando ViewModelProvider.
        // ViewModelProvider gestisce il ciclo di vita del ViewModel.
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        // Recupera l'Intent che ha avviato questa Activity.
        Intent intent = getIntent();
        // Controlla se l'Intent contiene l'extra EXTRA_NOTE_ID, indicando che si tratta di una modifica.
        if (intent.hasExtra(EXTRA_NOTE_ID)) {
            // Modifica nota esistente
            noteId = intent.getIntExtra(EXTRA_NOTE_ID, -1);// Recupera l'ID della nota.
            editTextTitle.setText(intent.getStringExtra(EXTRA_NOTE_TITLE));// Popola il campo titolo.
            editTextContent.setText(intent.getStringExtra(EXTRA_NOTE_CONTENT));// Popola il campo contenuto.
            // Imposta il titolo della Toolbar per riflettere l'operazione di modifica.
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Modifica Nota");
            }
        } else {
            // Nuova nota
            // Imposta il titolo della Toolbar per riflettere l'operazione di aggiunta.
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Aggiungi Nota");
            }
        }
    }

    /**
     * Inizializza i contenuti della barra delle opzioni standard dell'Activity.
     * Questo metodo viene chiamato per la prima volta per creare il menu,
     * e successivamente ogni volta che invalidateOptionsMenu() viene chiamato.
     * @param menu Il menu in cui inserire gli elementi.
     * @return true per visualizzare il menu; false altrimenti.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();// Ottiene un MenuInflater per convertire il XML del menu in oggetti Menu.
        menuInflater.inflate(R.menu.menu_add_edit_note, menu); // Inserisce il layout del menu (menu_add_edit_note.xml) nel Menu.
        return true;// Indica che il menu è stato creato e deve essere visualizzato.
    }

    /**
     * Questo hook viene chiamato ogni volta che un elemento nel menu delle opzioni selezionato.
     * Qui viene gestita l'azione di salvataggio della nota o il click sul pulsante "Indietro" della Toolbar.
     * @param item L'elemento del menu che è stato selezionato.
     * @return true se l'evento è stato gestito, false altrimenti.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();// Ottiene l'ID dell'elemento di menu cliccato.
        if (id == R.id.save_note) { // Se l'ID corrisponde all'icona di salvataggio definita in menu_add_edit_note.xml.
            saveNote();// Chiama il metodo per salvare la nota.
            return true;// L'evento è stato gestito.
        } else if (id == android.R.id.home) { // Se l'ID corrisponde al pulsante "Indietro"  della Toolbar.
            onBackPressed(); // Simula la pressione del tasto Indietro del sistema, tornando all'Activity precedente.
            return true;
        }
        // Per tutti gli altri elementi, chiama l'implementazione del genitore.
        return super.onOptionsItemSelected(item);
    }

    /**
     * Salva una nuova nota o aggiorna una nota esistente nel database.
     * Questo metodo recupera il titolo e il contenuto dagli EditText,
     * verifica che non siano vuoti e poi utilizza il {@link NoteViewModel}
     * per eseguire l'operazione di inserimento o aggiornamento nel database.
     */
    private void saveNote() {
        // Recupera il testo dal campo titolo e lo ripulisce da spazi iniziali/finali.
        String title = editTextTitle.getText().toString().trim();
        // Recupera il testo dal campo contenuto e lo ripulisce da spazi iniziali/finali.
        String content = editTextContent.getText().toString().trim();

        // Verifica se il titolo o il contenuto sono vuoti.
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            // Mostra un messaggio Toast all'utente se i campi sono vuoti.
            Toast.makeText(this, "Per favore, inserisci titolo e contenuto per la nota", Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis(); // Ottiene il timestamp corrente per la nota.

        // Controlla se si sta creando una nuova nota (noteId è -1) o modificandone una esistente.
        if (noteId == -1) {
            // Nuova nota// Nuova nota: crea un nuovo oggetto Note.
            Note newNote = new Note(title, content, timestamp);
            noteViewModel.insert(newNote);// Inserisce la nuova nota nel database tramite ViewModel.
            Toast.makeText(this, "Nota salvata!", Toast.LENGTH_SHORT).show();
        } else {
            // Aggiorna nota esistente: crea un oggetto Note con i nuovi dati.
            Note existingNote = new Note(title, content, timestamp);
            // È fondamentale impostare l'ID della nota esistente per l'operazione di aggiornamento.
            existingNote.setId(noteId);
            noteViewModel.update(existingNote);// Aggiorna la nota esistente nel database tramite ViewModel.
            Toast.makeText(this, "Nota aggiornata!", Toast.LENGTH_SHORT).show();
        }
        finish();// Chiudi l'Activity dopo che l'operazione di salvataggio/aggiornamento è completata.
    }
}