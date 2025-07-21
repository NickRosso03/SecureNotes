package com.example.securenotesapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.example.securenotesapp.database.NoteDatabase;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import com.example.securenotesapp.fragments.FileListFragment;
import com.example.securenotesapp.fragments.NotesFragment;


/**
 * {@code MainActivity} è la schermata principale dell'applicazione SecureNotes.
 * Gestisce la navigazione tra le diverse sezioni (NotesFragment, FileListFragment),
 * la toolbar, il drawer di navigazione e il Floating Action Button (FAB).
 * È anche responsabile della gestione del timeout di sessione per la sicurezza.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private DrawerLayout drawerLayout;// Il layout del drawer di navigazione.

    private NotesFragment notesFragment;    // Riferimento al NotesFragment
    private FileListFragment fileListFragment; // Riferimento al FileListFragment

    private FloatingActionButton fabAddNote;
    private FloatingActionButton fabAddFile;

    // Variabile per tenere traccia del fragment attualmente mostrato
    private Fragment activeFragment;

    // --- Variabili per il Timeout della Sessione ---
    private long SESSION_TIMEOUT_MS = 60 * 1000; // Durata del timeout di sessione in millisecondi
    private Handler sessionHandler;// Handler per gestire i callback del timeout di sessione.
    private Runnable sessionRunnable;// Runnable che viene eseguito allo scadere del timeout.
    private long lastInteractionTime=0; // Per registrare l'ultima interazione
    // -----------------------------------------------


    /**
     * Chiamato quando l'activity viene creata.
     * Inizializza gli elementi UI, configura il drawer di navigazione,
     * imposta i listener e avvia la gestione del timeout di sessione.
     *
     * @param savedInstanceState Se l'activity viene ricreata, contiene i dati salvati in precedenza.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);// Imposta la toolbar come ActionBar.

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Configura il toggle per aprire/chiudere il drawer di navigazione dalla toolbar.
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);  // Aggiunge il listener al drawer.
        toggle.syncState();// Sincronizza lo stato del toggle con il drawer.

        // Inizializzazione dei Floating Action Button
        fabAddNote = findViewById(R.id.fab_add_note);
        fabAddFile = findViewById(R.id.fab_add_file);

        // Listener per fabAddNote
        fabAddNote.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddEditNoteActivity.class);
            startActivity(intent);
        });

        // Listener per fabAddFile
        fabAddFile.setOnClickListener(view -> {
            // Controlla che fileListFragment sia stato inizializzato e sia il fragment attivo
            if (fileListFragment != null && activeFragment == fileListFragment) {
                fileListFragment.pickFile(); // Chiama il metodo per la selezione del file nel fragment
            } else {
                Log.e(TAG, "fabAddFile cliccato ma fileListFragment non è attivo o è null.");
                Toast.makeText(this, "Errore: il gestore file non è pronto o non visualizzato.", Toast.LENGTH_SHORT).show();
            }
        });


        // Carica il fragment di default (NotesFragment) se non ci sono stati salvati
        if (savedInstanceState == null) {
            notesFragment = new NotesFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, notesFragment, NotesFragment.class.getName()) // Usa un tag
                    .commit();
            activeFragment = notesFragment; // Imposta il fragment attivo
            updateFabVisibility(activeFragment); // Aggiorna la visibilità dei FAB
            getSupportActionBar().setTitle("Le mie Note Sicure"); // Imposta il titolo iniziale
        } else {
            // Se l'Activity viene ricreata (es. rotazione), recupera i riferimenti ai fragment esistenti
            // Usa findFragmentByTag per recuperare i fragment con i tag che abbiamo impostato
            notesFragment = (NotesFragment) getSupportFragmentManager().findFragmentByTag(NotesFragment.class.getName());
            fileListFragment = (FileListFragment) getSupportFragmentManager().findFragmentByTag(FileListFragment.class.getName());

            // Determina quale fragment era attivo per impostare 'activeFragment' e aggiornare i FAB
            Fragment currentFragmentInContainer = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragmentInContainer instanceof NotesFragment) {
                activeFragment = notesFragment;
                getSupportActionBar().setTitle("Le mie Note Sicure");
            } else if (currentFragmentInContainer instanceof FileListFragment) {
                activeFragment = fileListFragment;
                getSupportActionBar().setTitle("I miei File Criptati");
            }
            updateFabVisibility(activeFragment); // Aggiorna la visibilità anche dopo la ricreazione
        }

        // --- Inizializzazione del Timeout della Sessione ---
        sessionHandler = new Handler();
        sessionRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Session timeout reached. Returning to LoginActivity.");
                // Chiude il database prima di tornare al login per rilasciare le risorse e rafforzare la sicurezza
                NoteDatabase.closeDatabase();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        };
        SESSION_TIMEOUT_MS = PreferenceManager.getDefaultSharedPreferences(this)
                .getLong(SettingsActivity.KEY_SESSION_TIMEOUT, 180000);

        // L'ultima interazione è ora, all'avvio dell'Activity
        lastInteractionTime = System.currentTimeMillis();
        startSessionTimeout(); // Avvia il timer all'avvio dell'Activity
    }

    // --- Metodi per la gestione del Timeout della Sessione ---

    private void startSessionTimeout() {
        sessionHandler.removeCallbacks(sessionRunnable); // Rimuove eventuali callback pendenti
        long delay = SESSION_TIMEOUT_MS - (System.currentTimeMillis() - lastInteractionTime);
        if (delay < 0) { // Se il tempo di inattività ha già superato il timeout
            delay = 0;
        }
        sessionHandler.postDelayed(sessionRunnable, delay);
        Log.d(TAG, "Session timeout avviato/resettato con un delay di: " + delay + "ms");
    }

    private void resetInteractionTime() {
        lastInteractionTime = System.currentTimeMillis();
        startSessionTimeout();
        Log.d(TAG, "Interazione rilevata: timeout resettato.");
    }

    /**
     * Chiamato quando un elemento del drawer di navigazione viene selezionato.
     * Gestisce la navigazione ai diversi fragment (Notes, Files, Settings)
     * e l'azione di logout.
     *
     * @param item L'elemento del menu selezionato.
     * @return true se l'elemento è stato gestito, false altrimenti.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        String fragmentTag = null; // Tag per il fragment

        int id = item.getItemId(); // Recupera l'ID dell'elemento cliccato

        if (id == R.id.nav_notes) {
            if (notesFragment == null) {
                notesFragment = new NotesFragment();
            }
            selectedFragment = notesFragment;
            fragmentTag = NotesFragment.class.getName();
            getSupportActionBar().setTitle("Le mie Note Sicure"); // Aggiorna il titolo della Toolbar
        } else if (id == R.id.nav_files) {
            if (fileListFragment == null) {
                fileListFragment = new FileListFragment();
            }
            selectedFragment = fileListFragment;
            fragmentTag = FileListFragment.class.getName();
            getSupportActionBar().setTitle("I miei File Criptati"); // Aggiorna il titolo della Toolbar
        } else if (id == R.id.nav_settings) {
            // Avvia la SettingsActivity
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class); // Assicurati che 'SettingsActivity.class' sia il nome corretto della tua activity
            startActivity(intent);
            drawerLayout.closeDrawer(GravityCompat.START); // Chiudi il drawer dopo aver avviato l'Activity
            return true;
        } else if (id == R.id.nav_logout) {
            Toast.makeText(this, "Logout...", Toast.LENGTH_SHORT).show();
            // Implementa la logica di logout
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }

        // Sostituisci il fragment solo se è diverso da quello attualmente attivo
        // e se selectedFragment non è null (cioè non è una voce come "Impostazioni" senza fragment)
        if (selectedFragment != null && selectedFragment != activeFragment) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment, fragmentTag) // Usa il tag per il replace
                    .commit();
            activeFragment = selectedFragment; // Aggiorna il fragment attivo
            updateFabVisibility(activeFragment); // Aggiorna la visibilità dei FAB
        } else if (selectedFragment == null) {
            // Se selectedFragment è null (es. per Settings non implementate), aggiorna comunque i FAB
            // per nasconderli o gestirli di conseguenza
            updateFabVisibility(null); // Passa null per nascondere tutti i FAB
            activeFragment = null; // Nessun fragment attivo riconosciuto
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Aggiorna la visibilità del Floating Action Button (FAB) in base al fragment attualmente visualizzato.
     * Il FAB è visibile solo per {@link NotesFragment} e {@link FileListFragment}.
     *
     * @param currentFragment Il fragment attualmente in mostra.
     */
    private void updateFabVisibility(Fragment currentFragment) {
        if (fabAddNote == null || fabAddFile == null) {
            // Questo potrebbe accadere se il metodo viene chiamato prima di onCreate
            Log.w(TAG, "FABs not yet initialized in updateFabVisibility.");
            return;
        }

        if (currentFragment instanceof NotesFragment) {
            fabAddNote.setVisibility(View.VISIBLE);
            fabAddFile.setVisibility(View.GONE);
            Log.d(TAG, "Showing Note FAB, Hiding File FAB");
        } else if (currentFragment instanceof FileListFragment) {
            fabAddNote.setVisibility(View.GONE);
            fabAddFile.setVisibility(View.VISIBLE);
            Log.d(TAG, "Showing File FAB, Hiding Note FAB");
        } else {
            // Nascondi entrambi se non è né note né file (es. settings o nessun fragment caricato)
            fabAddNote.setVisibility(View.GONE);
            fabAddFile.setVisibility(View.GONE);
            Log.d(TAG, "Hiding all FABs");
        }
    }


    /**
     * Intercetta tutti gli eventi touch per reimpostare il timer di timeout della sessione
     * ogni volta che l'utente interagisce con l'applicazione.
     *
     * @param ev L'evento di movimento.
     * @return true se l'evento è stato gestito, false altrimenti.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            resetInteractionTime(); // Resetta il tempo dell'ultima interazione
        }
        return super.dispatchTouchEvent(ev);
    }
    // --------------------------------------------------------
    /**
     * Chiamato quando l'activity torna in primo piano.
     * Ricontrolla il timeout di sessione e lo avvia/riavvia.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Ricarica SESSION_TIMEOUT_MS nel caso sia stato modificato nelle impostazioni.
        SESSION_TIMEOUT_MS = PreferenceManager.getDefaultSharedPreferences(this)
                .getLong(SettingsActivity.KEY_SESSION_TIMEOUT, 180000);

        // Se il tempo di inattività supera il timeout, esegue il logout.
        if (lastInteractionTime > 0 && System.currentTimeMillis() - lastInteractionTime >= SESSION_TIMEOUT_MS) {
            Log.d(TAG, "Timeout scaduto durante il background. Eseguo logout.");
            sessionRunnable.run();
        } else {
            startSessionTimeout();
        }
        Log.d(TAG, "MainActivity onResume.");
    }


    /**
     * Chiamato quando l'activity passa in background o un'altra activity viene in primo piano.
     * Ferma il timer di timeout e registra l'ora corrente come ultima interazione.
     */
    @Override
    protected void onPause() {
        super.onPause();
        sessionHandler.removeCallbacks(sessionRunnable); // Ferma il timer per evitare che si attivi mentre siamo in background
        // Registra il tempo in cui l'app va in background
        lastInteractionTime = System.currentTimeMillis();
        Log.d(TAG, "MainActivity onPause: tempo ultima interazione registrato.");
    }
    /**
     * Chiamato quando l'activity viene distrutta.
     * Assicura che qualsiasi callback pendente del timeout di sessione venga rimosso
     * per prevenire memory leak.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        sessionHandler.removeCallbacks(sessionRunnable);
        Log.d(TAG, "MainActivity onDestroy: timeout rimosso.");
    }
    /**
     * Gestisce la pressione del tasto "Indietro" del dispositivo.
     * Se il drawer di navigazione è aperto, lo chiude. Altrimenti, esegue l'azione predefinita.
     */

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

}