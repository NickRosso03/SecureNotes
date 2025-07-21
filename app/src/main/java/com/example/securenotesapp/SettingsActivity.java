package com.example.securenotesapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.example.securenotesapp.utils.BackupManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@code SettingsActivity} gestisce le impostazioni dell'applicazione, permettendo all'utente di
 * configurare il timeout della sessione, gestire il PIN di accesso, ed eseguire operazioni di
 * backup e ripristino dei dati cifrati.
 *
 * Questa Activity interagisce con {@link KeyManager} per la gestione del PIN e con {@link BackupManager}
 * per le operazioni di backup/ripristino.
 */

public class SettingsActivity extends AppCompatActivity {
    // Chiave per salvare/recuperare il timeout della sessione dalle SharedPreferences.
    public static final String KEY_SESSION_TIMEOUT = "session_timeout_ms";
    private EditText editTextSessionTimeout;
    private Button buttonExportBackup;
    private Button buttonImportBackup;
    // Variabili per la gestione del PIN
    private EditText editTextPin;
    private EditText editTextPinConfirm;
    private Button buttonSetPin;
    private Button buttonClearPin;

    private KeyManager keyManager; // Gestore delle chiavi e del PIN

    private BackupManager backupManager; // Gestore delle operazioni di backup.
    // Executor per eseguire operazioni lunghe (backup/ripristino) in background.
    private ExecutorService executorService;
    private Handler mainHandler;

    private static final String TAG = "SettingsActivity";

    // Launcher per la selezione del file di output per l'esportazione del backup.
    private ActivityResultLauncher<Intent> createBackupFileLauncher;
    // Launcher per la selezione del file di input per l'importazione del backup.
    private ActivityResultLauncher<Intent> openBackupFileLauncher;

    /**
     * Chiamato quando l'activity viene creata.
     * Inizializza gli elementi UI, imposta i listener e prepara i launcher per i file.
     *
     * @param savedInstanceState Se l'activity viene ricreata, contiene i dati precedentemente salvati.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Configura la toolbar.
        Toolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        editTextSessionTimeout = findViewById(R.id.edit_text_session_timeout);
        buttonExportBackup = findViewById(R.id.button_export_backup);
        buttonImportBackup = findViewById(R.id.button_import_backup);
        // Inizializza il KeyManager
        keyManager = new KeyManager(this);

        // Inizializza gli elementi UI del PIN
        editTextPin = findViewById(R.id.edit_text_pin);
        editTextPinConfirm = findViewById(R.id.edit_text_pin_confirm);
        buttonSetPin = findViewById(R.id.button_set_pin);
        buttonClearPin = findViewById(R.id.button_clear_pin);

        // Imposta i listener per i pulsanti del PIN
        buttonSetPin.setOnClickListener(v -> setPin());
        buttonClearPin.setOnClickListener(v -> clearPin());

        // Aggiorna lo stato iniziale dei pulsanti/campi del PIN
        updatePinUIState();

        long currentTimeout = PreferenceManager.getDefaultSharedPreferences(this)
                .getLong(KEY_SESSION_TIMEOUT, 180000);
        editTextSessionTimeout.setText(String.valueOf(currentTimeout / 1000));

        // Inizializza BackupManager e gli executor
        backupManager = new BackupManager(this);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // Inizializza i ActivityResultLauncher
        setupActivityResultLaunchers();

        // Listener per il pulsante Esporta Backup
        buttonExportBackup.setOnClickListener(v -> showPasswordDialogAndExport());

        // Listener per il pulsante Importa Backup
        buttonImportBackup.setOnClickListener(v -> showPasswordDialogAndImport());
    }

    /**
     * Configura gli ActivityResultLauncher per la selezione e la creazione di file.
     * Questi launcher permettono di avviare Intent per selezionare o creare file
     * e gestire i risultati.
     */
    private void setupActivityResultLaunchers() {
        // Launcher per creare il file di backup. Si occupa di ricevere il risultato
        // dopo che l'utente ha scelto dove salvare il file.
        createBackupFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri uri = data.getData();
                            // Chiedi la password e avvia l'esportazione
                            showPasswordDialogForExportConfirmation(uri);
                        } else {
                            Toast.makeText(this, "URI di salvataggio non valido.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Salvataggio backup annullato.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Launcher per aprire il file di backup. Si occupa di ricevere il risultato
        // dopo che l'utente ha selezionato un file da importare.
        openBackupFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri uri = data.getData();// URI del file scelto dall'utente.
                            // Chiedi la password e avvia l'importazione
                            showPasswordDialogForImportConfirmation(uri);
                        } else {
                            Toast.makeText(this, "URI di importazione non valido.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Importazione backup annullata.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Mostra un dialog per richiedere la password prima di avviare la creazione del file di backup.
     * Se la password è valida, procede con la selezione del percorso di salvataggio del file.
     */
    private void showPasswordDialogAndExport() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Password Backup");
        builder.setMessage("Inserisci una password per crittografare il tuo backup. Ricordala bene!");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Crea Backup", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, "La password non può essere vuota.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Avvia la selezione del percorso di salvataggio del file
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip"); // Tipo MIME per file zip
            intent.putExtra(Intent.EXTRA_TITLE, "SecureNotes_backup_" + System.currentTimeMillis() + ".zip");
            createBackupFileLauncher.launch(intent);
        });
        builder.setNegativeButton("Annulla", (dialog, which) -> dialog.cancel());

        builder.show();
    }
    /**
     * Mostra un dialog per richiedere la conferma della password prima di avviare l'esportazione effettiva.
     *
     * @param uri L'URI del file dove salvare il backup.
     */
    private void showPasswordDialogForExportConfirmation(Uri uri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Conferma Password");
        builder.setMessage("Reinserisci la password per confermare la crittografia.");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Conferma", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, "La password non può essere vuota.", Toast.LENGTH_SHORT).show();
                return;
            }
            startExportBackup(uri, password);
        });
        builder.setNegativeButton("Annulla", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Avvia l'operazione di esportazione del backup in un thread separato, mostrando una ProgressBar.
     *
     * @param outputUri L'URI del file dove salvare il backup.
     * @param password La password per crittografare il backup.
     */
    private void startExportBackup(Uri outputUri, String password) {
        // Mostra un AlertDialog con ProgressBar per l'avanzamento
        AlertDialog.Builder progressDialogBuilder = new AlertDialog.Builder(this);
        progressDialogBuilder.setTitle("Esportazione Backup");
        progressDialogBuilder.setCancelable(false); // Impedisce la chiusura durante il processo

        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(false);
        progressBar.setMax(100);

        TextView progressMessage = new TextView(this);
        progressMessage.setPadding(30, 30, 30, 30);
        progressMessage.setText("In preparazione...");

        progressDialogBuilder.setView(progressBar);
        progressDialogBuilder.setView(progressMessage);

        AlertDialog progressDialog = progressDialogBuilder.create();
        progressDialog.show();

        // Esegui l'operazione di backup in un thread separato per non bloccare l'UI.
        executorService.execute(() -> {
            boolean success = backupManager.exportBackup(outputUri, password, (progress, message) -> {
                mainHandler.post(() -> {
                    // Aggiorna la ProgressBar e il messaggio sulla UI thread.
                    progressBar.setProgress(progress);
                    progressMessage.setText(message);
                });
            });
            // Una volta completata l'operazione, torna sulla UI thread per mostrare il risultato.
            mainHandler.post(() -> {
                progressDialog.dismiss();
                if (success) {
                    Toast.makeText(this, "Backup esportato con successo!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Errore durante l'esportazione del backup.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /**
     * Mostra un dialog per richiedere la password prima di avviare la selezione del file di backup da importare.
     * Se la password è valida, procede con la selezione del file.
     */
    private void showPasswordDialogAndImport() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Password Ripristino");
        builder.setMessage("Inserisci la password del file di backup per decrittografarlo.");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Importa", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, "La password non può essere vuota.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Avvia la selezione del file di backup
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip"); // Può essere anche "*/*" se non sai il tipo esatto
            openBackupFileLauncher.launch(intent);
        });
        builder.setNegativeButton("Annulla", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    /**
     * Mostra un dialog per richiedere la conferma della password prima di avviare l'importazione effettiva.
     *
     * @param uri L'URI del file di backup da importare.
     */
    private void showPasswordDialogForImportConfirmation(Uri uri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Conferma Password");
        builder.setMessage("Reinserisci la password per confermare la decrittografia.");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Conferma", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, "La password non può essere vuota.", Toast.LENGTH_SHORT).show();
                return;
            }
            startImportBackup(uri, password);
        });
        builder.setNegativeButton("Annulla", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Avvia l'operazione di importazione del backup in un thread separato, mostrando una ProgressBar. [cite: 54]
     *
     * @param inputUri L'URI del file di backup da importare.
     * @param password La password per decrittografare il backup.
     */
    private void startImportBackup(Uri inputUri, String password) {
        // Mostra un AlertDialog con ProgressBar per l'avanzamento
        AlertDialog.Builder progressDialogBuilder = new AlertDialog.Builder(this);
        progressDialogBuilder.setTitle("Importazione Backup");
        progressDialogBuilder.setCancelable(false); // Impedisce la chiusura durante il processo

        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(false);
        progressBar.setMax(100);

        TextView progressMessage = new TextView(this);
        progressMessage.setPadding(30, 30, 30, 30);
        progressMessage.setText("In preparazione...");

        progressDialogBuilder.setView(progressBar);
        progressDialogBuilder.setView(progressMessage);


        AlertDialog progressDialog = progressDialogBuilder.create();
        progressDialog.show();

        // Esegui l'operazione di ripristino in un thread separato
        executorService.execute(() -> {
            boolean success = backupManager.importBackup(inputUri, password, (progress, message) -> {
                mainHandler.post(() -> {
                    progressBar.setProgress(progress);
                    progressMessage.setText(message);
                });
            });

            mainHandler.post(() -> {
                progressDialog.dismiss();
                if (success) {
                    Toast.makeText(this, "Backup importato con successo!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Errore durante l'importazione del backup. Controlla la password.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /**
     * Salva il valore del timeout di sessione inserito dall'utente nelle SharedPreferences.
     * Gestisce anche la validazione dell'input.
     */
    private void saveSessionTimeout() {
        String timeoutStr = editTextSessionTimeout.getText().toString();
        if (timeoutStr.isEmpty()) {
            Toast.makeText(this, "Il timeout non può essere vuoto", Toast.LENGTH_SHORT).show();
            // Opzionale: reimposta al valore precedente o un default
            editTextSessionTimeout.setText(String.valueOf(PreferenceManager.getDefaultSharedPreferences(this)
                    .getLong(KEY_SESSION_TIMEOUT, 180000) / 1000));
            return;
        }

        try {
            int timeoutInSeconds = Integer.parseInt(timeoutStr);
            if (timeoutInSeconds <= 0) {
                Toast.makeText(this, "Il timeout deve essere maggiore di 0", Toast.LENGTH_SHORT).show();
                // Reimposta al valore precedente o un default valido
                editTextSessionTimeout.setText(String.valueOf(PreferenceManager.getDefaultSharedPreferences(this)
                        .getLong(KEY_SESSION_TIMEOUT, 180000) / 1000));
                return;
            }
            long timeoutInMillis = timeoutInSeconds * 1000;
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putLong(KEY_SESSION_TIMEOUT, timeoutInMillis)
                    .apply();
            Toast.makeText(this, "Timeout di sessione salvato!", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Inserisci un numero valido per il timeout", Toast.LENGTH_SHORT).show();
            // Reimposta al valore precedente
            editTextSessionTimeout.setText(String.valueOf(PreferenceManager.getDefaultSharedPreferences(this)
                    .getLong(KEY_SESSION_TIMEOUT, 180000) / 1000));
        }
    }
    /**
     * Chiamato quando l'activity sta per essere messa in background o non è più visibile.
     * In questo caso, salva automaticamente il timeout di sessione.
     */
    @Override
    protected void onPause() {
        super.onPause();
        saveSessionTimeout();
    }

    /**
     * Chiamato quando l'activity sta per essere distrutta.
     * Assicura che l'ExecutorService venga spento correttamente per evitare memory leak.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Spegne l'ExecutorService quando l'attività viene distrutta
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }

    /**
     * Gestisce l'azione del pulsante "Indietro" nella toolbar.
     *
     * @return true se l'evento è stato gestito, false altrimenti.
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    // --- Metodi per la gestione del PIN ---

    /**
     * Imposta o cambia il PIN di accesso dell'applicazione.
     * Esegue controlli sulla lunghezza e sulla corrispondenza dei PIN inseriti.
     */
    private void setPin() {
        String pin = editTextPin.getText().toString();
        String confirmPin = editTextPinConfirm.getText().toString();

        if (pin.isEmpty() || confirmPin.isEmpty()) {
            Toast.makeText(this, "Per favore, inserisci e conferma il PIN.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pin.length() != 4) {
            Toast.makeText(this, "Il PIN deve essere di 4 cifre.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pin.equals(confirmPin)) {
            Toast.makeText(this, "I PIN non corrispondono.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Salva il PIN usando il KeyManager
        if (keyManager.savePin(pin)) {
            Toast.makeText(this, "PIN impostato con successo!", Toast.LENGTH_SHORT).show();
            editTextPin.setText(""); // Pulisci i campi
            editTextPinConfirm.setText("");
            updatePinUIState(); // Aggiorna lo stato della UI
        } else {
            Toast.makeText(this, "Errore durante il salvataggio del PIN.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Rimuove il PIN di accesso dall'applicazione, dopo una richiesta di conferma all'utente.
     */
    private void clearPin() {
        new AlertDialog.Builder(this)
                .setTitle("Conferma rimozione PIN")
                .setMessage("Sei sicuro di voler rimuovere il PIN di accesso? Dovrai usare solo la biometria.")
                .setPositiveButton("Sì", (dialog, which) -> {
                    keyManager.clearPin();
                    Toast.makeText(this, "PIN rimosso.", Toast.LENGTH_SHORT).show();
                    updatePinUIState(); // Aggiorna lo stato della UI
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Aggiorna lo stato degli elementi UI del PIN in base alla presenza di un PIN salvato.
     */
    private void updatePinUIState() {
        if (keyManager.hasPin()) {
            // Se un PIN esiste, l'utente può cambiarlo o rimuoverlo
            buttonSetPin.setText("Cambia PIN");
            buttonClearPin.setVisibility(View.VISIBLE);
        } else {
            // Se non esiste un PIN, l'utente può impostarlo
            buttonSetPin.setText("Imposta PIN");
            buttonClearPin.setVisibility(View.GONE);
        }
        editTextPin.setHint(keyManager.hasPin() ? "Inserisci il nuovo PIN (4 cifre)" : "Imposta il tuo PIN (4 cifre)");
        editTextPinConfirm.setHint(keyManager.hasPin() ? "Conferma il nuovo PIN" : "Conferma il tuo PIN");
    }

}