package com.example.securenotesapp.fragments;

import static com.example.securenotesapp.FileManager.ENCRYPTED_FILES_DIR;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securenotesapp.R;
import com.example.securenotesapp.SecureNotesApplication;
import com.example.securenotesapp.adapter.FileAdapter;
import com.example.securenotesapp.model.FileItem;
import com.example.securenotesapp.viewmodel.FileViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;
import android.webkit.MimeTypeMap;

/**
 * Fragment responsabile della visualizzazione di un elenco di file sicuri
 * e della gestione delle operazioni correlate come l'aggiunta, l'apertura e l'eliminazione di file.
 * Interagisce con il {@link FileViewModel} per recuperare e gestire i dati dei file
 * e con FileManager per le operazioni di crittografia/decrittografia dei file sul filesystem.
 */
public class FileListFragment extends Fragment implements FileAdapter.OnItemClickListener {

    private static final String TAG = "FileListFragment";
    private FileViewModel fileViewModel;
    private FileAdapter adapter;
    private TextView textViewNoFiles;
    private SecureNotesApplication application;
    private ActivityResultLauncher<String[]> filePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                if (isAdded() && getContext() != null) {
                    handleSelectedFile(uri);
                }
            } else {
                Toast.makeText(getContext(), "Nessun file selezionato.", Toast.LENGTH_SHORT).show();
            }
        });

        application = (SecureNotesApplication) requireActivity().getApplication();
    }

    /**
     * Chiamato per creare e restituire la gerarchia di View associata al Fragment.
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
        View view = inflater.inflate(R.layout.fragment_file_list, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_files);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        textViewNoFiles = view.findViewById(R.id.text_view_no_files);

        adapter = new FileAdapter(getContext());
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(this);

        fileViewModel = new ViewModelProvider(this).get(FileViewModel.class);
        fileViewModel.getAllFileItems().observe(getViewLifecycleOwner(), new Observer<List<FileItem>>() {
            @Override
            public void onChanged(List<FileItem> fileItems) {
                adapter.setFileItems(fileItems);
                updateNoFilesVisibility(fileItems.isEmpty());
            }
        });

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                FileItem deletedFile = adapter.getFileItemAt(position);
                fileViewModel.delete(deletedFile);
                Toast.makeText(getContext(), "File eliminato", Toast.LENGTH_SHORT).show();

                // Elimina fisicamente il file crittografato utilizzando FileManager
                // Passa il percorso assoluto, FileManager lo gestirà.
                application.getFileManager().fileIOExecutor.execute(() -> {
                    application.getFileManager().deleteEncryptedFile(deletedFile.getEncryptedFilePath());
                });

                Snackbar.make(view, "File eliminato", Snackbar.LENGTH_LONG)
                        .setAction("ANNULLA", v -> {
                            fileViewModel.insert(deletedFile);
                        }).show();
            }
        }).attachToRecyclerView(recyclerView);

        return view;
    }

    @Override
    public void onItemClick(FileItem fileItem) {
        openEncryptedFile(fileItem);
    }

    public void pickFile() {
        filePickerLauncher.launch(new String[]{"*/*"});
    }

    /**
     * Gestisce il file URI selezionato dall'utente.
     * Estrae i metadati del file (nome, dimensione) e avvia l'operazione di
     * copia e crittografia in un thread in background. Successivamente, salva
     * le informazioni del file nel database.
     *
     * @param fileUri L'URI del file selezionato dall'utente.
     */
    private void handleSelectedFile(Uri fileUri) {
        application.getFileManager().fileIOExecutor.execute(() -> {
            ContentResolver contentResolver = requireContext().getContentResolver();
            String originalFileName = null;
            String mimeType = null;
            long fileSize = 0;

            // Ottiene dettagli del file direttamente dal ContentResolver
            try (Cursor cursor = contentResolver.query(fileUri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        originalFileName = cursor.getString(nameIndex);
                    }
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex);
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Errore nell'ottenere i dettagli del file da ContentResolver: " + e.getMessage());
            }

            mimeType = contentResolver.getType(fileUri);

            // Se il nome del file non è stato trovato o è vuoto, usa un fallback
            if (originalFileName == null || originalFileName.isEmpty()) {
                originalFileName = "unnamed_file_" + System.currentTimeMillis();
                if (mimeType != null) {
                    String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                    if (extension != null) {
                        originalFileName += "." + extension;
                    }
                }
            }

            try (InputStream inputStream = contentResolver.openInputStream(fileUri)) {
                if (inputStream != null) {
                    String encryptedRelativePath = null;
                    try {
                        // Questo metodo gestirà la creazione del file crittografato e restituirà il suo percorso relativo
                        encryptedRelativePath = application.getFileManager().encryptAndSaveFile(inputStream);

                        // Poiché encryptAndSaveFile restituisce un percorso *relativo*,
                        // dobbiamo costruire il percorso assoluto per il FileItem.
                        // La directory è context.getFilesDir() + ENCRYPTED_FILES_DIR
                        File encryptedFilesBaseDir = new File(requireContext().getFilesDir(), "encrypted_files");
                        String absoluteEncryptedFilePath = new File(encryptedFilesBaseDir, encryptedRelativePath).getAbsolutePath();


                        // Crea FileItem e lo salva nel database
                        FileItem newFileItem = new FileItem(originalFileName, mimeType, absoluteEncryptedFilePath, fileSize, System.currentTimeMillis());
                        fileViewModel.insert(newFileItem);

                        new Handler(Looper.getMainLooper()).post(() ->
                                Toast.makeText(getContext(), "File '" + newFileItem.getOriginalFileName() + "' aggiunto e criptato!", Toast.LENGTH_LONG).show());

                    } catch (GeneralSecurityException | IOException e) {
                        Log.e(TAG, "Errore durante la crittografia del file: " + e.getMessage(), e);
                        new Handler(Looper.getMainLooper()).post(() ->
                                Toast.makeText(getContext(), "Errore di crittografia del file.", Toast.LENGTH_SHORT).show());
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Errore nella lettura del file selezionato: " + e.getMessage(), e);
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getContext(), "Errore nella lettura del file selezionato.", Toast.LENGTH_SHORT).show());
            }
        });
    }
    /**
     * Apre un {@link FileItem} decifrandolo e visualizzandolo con un'applicazione esterna.
     * Il processo include la decifratura del file in un percorso temporaneo e l'invio di un
     * {@link Intent#ACTION_VIEW} con un {@link Uri} generato tramite il FileProvider.
     *
     * @param fileItem L'oggetto {@link FileItem} da aprire.
     */
    private void openEncryptedFile(FileItem fileItem) {
        application.getFileManager().fileIOExecutor.execute(() -> {
            try {
                // Dobbiamo estrarre il percorso relativo dal percorso assoluto salvato nel FileItem
                String absolutePath = fileItem.getEncryptedFilePath();
                String relativePath = null;
                File filesDir = requireContext().getFilesDir();
                File encryptedFilesBaseDir = new File(filesDir, ENCRYPTED_FILES_DIR);

                if (absolutePath.startsWith(encryptedFilesBaseDir.getAbsolutePath())) {
                    relativePath = absolutePath.substring(encryptedFilesBaseDir.getAbsolutePath().length() + 1);
                } else {
                    Log.e(TAG, "Il percorso del file criptato non corrisponde alla directory prevista: " + absolutePath);
                    // Fallback: usa solo il nome del file se il percorso non è come previsto
                    relativePath = new File(absolutePath).getName();
                }

                if (relativePath == null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(getContext(), "Errore: Percorso file criptato non valido.", Toast.LENGTH_SHORT).show());
                    return;
                }

                final Uri fileUriToOpen = application.getFileManager().decryptAndOpenFile(relativePath, fileItem.getOriginalFileName());

                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(fileUriToOpen, fileItem.getMimeType());
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                            startActivity(intent);
                        } else {
                            Toast.makeText(getContext(), "Nessuna applicazione trovata per aprire questo tipo di file.", Toast.LENGTH_LONG).show();
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "FileProvider error (from FileManager): " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Errore di sicurezza nell'apertura del file.", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Errore nell'apertura del file decifrato: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Impossibile aprire il file.", Toast.LENGTH_SHORT).show();
                    }
                    // Non c'è bisogno di un blocco finally per eliminare il file temporaneo qui,
                    // perché FileManager.decryptAndOpenFile gestisce la creazione di un file temporaneo
                    // e la pulizia è gestita da FileManager.cleanTempFiles() (es. in onTerminate dell'Application)
                });

            } catch (GeneralSecurityException | IOException e) {
                Log.e(TAG, "Errore durante la decrittografia del file: " + e.getMessage(), e);
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getContext(), "Errore di decrittografia del file.", Toast.LENGTH_SHORT).show());
            }
        });
    }
    /**
     * Aggiorna la visibilità del messaggio ("Nessun file presente")
     * e della {@link RecyclerView} in base alla presenza di elementi nella lista.
     *
     * @param isEmpty Vero se la lista dei file è vuota, Falso altrimenti.
     */
    private void updateNoFilesVisibility(boolean isEmpty) {
        if (isEmpty) {
            textViewNoFiles.setVisibility(View.VISIBLE);
        } else {
            textViewNoFiles.setVisibility(View.GONE);
        }
    }
}