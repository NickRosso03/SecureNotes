package com.example.securenotesapp.viewmodel;
import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.securenotesapp.SecurityManager; // Necessario per ottenere la passphrase del DB
import com.example.securenotesapp.dao.FileDao;
import com.example.securenotesapp.database.NoteDatabase;
import com.example.securenotesapp.model.FileItem;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository per la gestione dei dati relativi agli elementi file ({@link FileItem}).
 * Questa classe astrae la fonte dati (il database Room) e fornisce un'API pulita
 * per accedere ai dati dei file. Gestisce le operazioni sul database in un thread separato
 * per evitare di bloccare il thread principale (UI thread).
 *
 * Utilizza {@link LiveData} per fornire dati osservabili all'interfaccia utente,
 * garantendo che l'UI si aggiorni automaticamente quando i dati sottostanti cambiano.
 * Si interfaccia con {@link FileDao} per le operazioni dirette sul database.
 */
public class FileRepository {
    private FileDao fileDao;// Data Access Object per i file, usato per interagire con il database.
    private LiveData<List<FileItem>> allFileItems;// LiveData che contiene tutti gli elementi file.
    // Pool di thread per eseguire operazioni sul database in background, separatamente dal thread principale.
    private ExecutorService databaseExecutor;
    /**
     * Costruttore per il FileRepository.
     * Inizializza il database e i DAO, e recupera la passphrase del database
     * tramite {@link SecurityManager} per garantire la sicurezza del database SQLCipher.
     *
     * @param application L'istanza dell'applicazione, necessaria per accedere al contesto e inizializzare il database.
     * @throws RuntimeException Se la passphrase del database non può essere recuperata o se c'è un errore
     * nell'inizializzazione del database.
     */
    public FileRepository(Application application) {
        // Inizializza un pool di 4 thread per eseguire le operazioni sul database in parallelo.
        databaseExecutor = Executors.newFixedThreadPool(4);

        try {
            // Ottieni la passphrase del database tramite SecurityManager
            SecurityManager securityManager = new SecurityManager(application);
            byte[] passphrase = securityManager.getDatabasePassphrase();

            // Inizializza il database Room, passando la passphrase per SQLCipher.
            NoteDatabase database = NoteDatabase.getDatabase(application, passphrase);
            // Ottiene l'istanza del FileDao dal database.
            fileDao = database.fileDao();
            // Ottiene un LiveData con tutti gli elementi file presenti nel database.
            allFileItems = fileDao.getAllFileItems();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Errore nell'inizializzazione del FileRepository o del database.", e);
        }
    }

    /**
     * Restituisce un {@link LiveData} contenente una lista di tutti gli elementi file.
     * Questo LiveData può essere osservato dall'interfaccia utente per ricevere
     * aggiornamenti automatici quando la lista di file nel database cambia.
     *
     * @return Un LiveData con la lista di {@link FileItem}.
     */
    public LiveData<List<FileItem>> getAllFileItems() {
        return allFileItems;
    }

    // Metodi per le operazioni CRUD (Create, Read, Update, Delete)
    // Eseguiti su un thread separato

    /**
     * Inserisce un nuovo elemento file nel database.
     * L'operazione viene eseguita in un thread separato gestito da {@code databaseExecutor}.
     *
     * @param fileItem L'oggetto {@link FileItem} da inserire.
     */
    public void insert(FileItem fileItem) {
        databaseExecutor.execute(() -> fileDao.insert(fileItem));
    }
    /**
     * Aggiorna un elemento file esistente nel database.
     * L'operazione viene eseguita in un thread separato gestito da {@code databaseExecutor}.
     *
     * @param fileItem L'oggetto {@link FileItem} da aggiornare.
     */
    public void update(FileItem fileItem) {
        databaseExecutor.execute(() -> fileDao.update(fileItem));
    }
    /**
     * Elimina un elemento file dal database.
     * L'operazione viene eseguita in un thread separato gestito da {@code databaseExecutor}.
     *
     * @param fileItem L'oggetto {@link FileItem} da eliminare.
     */
    public void delete(FileItem fileItem) {
        databaseExecutor.execute(() -> fileDao.delete(fileItem));
    }
    /**
     * Restituisce un {@link LiveData} contenente un singolo elemento file basato sul suo ID.
     * Utile per osservare i cambiamenti di un file specifico.
     *
     * @param fileItemId L'ID dell'elemento file da recuperare.
     * @return Un LiveData con l'oggetto {@link FileItem} corrispondente all'ID.
     */
    public LiveData<FileItem> getFileItemById(int fileItemId) {
        return fileDao.getFileItemById(fileItemId);
    }
}