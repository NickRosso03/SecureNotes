package com.example.securenotesapp.viewmodel; // Lo stesso package di NoteViewModel

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.securenotesapp.model.FileItem;

import java.util.List;

/**
 * ViewModel per la gestione dei dati degli elementi file ({@link FileItem}) nell'interfaccia utente.
 * Questa classe agisce da intermediario tra l'interfaccia utente (Activity/Fragment)
 * e il {@link FileRepository}, fornendo dati osservabili e gestendo le operazioni
 * sul database in modo da essere indipendente dal ciclo di vita dell'Activity.
 *
 * Estende {@link AndroidViewModel} per avere accesso al contesto dell'applicazione.
 */
public class FileViewModel extends AndroidViewModel {
    private FileRepository repository; // Il Repository che gestisce l'accesso ai dati.
    private LiveData<List<FileItem>> allFileItems;// LiveData che contiene la lista di tutti gli elementi file.

    /**
     * Costruttore per il FileViewModel.
     * Inizializza il {@link FileRepository} e recupera il {@link LiveData}
     * contenente tutti gli elementi file.
     *
     * @param application L'istanza dell'applicazione.
     */
    public FileViewModel(Application application) {
        super(application);// Chiama il costruttore della classe genitore AndroidViewModel.
        repository = new FileRepository(application); // Inizializza il repository con il contesto dell'applicazione.
        allFileItems = repository.getAllFileItems(); // Ottiene il LiveData con tutti gli elementi file dal repository.
    }

    /**
     * Restituisce un {@link LiveData} contenente una lista di tutti gli elementi file.
     * Questo metodo è il punto di accesso per l'interfaccia utente per osservare
     * i cambiamenti nella lista dei file.
     *
     * @return Un LiveData con la lista di {@link FileItem}.
     */
    public LiveData<List<FileItem>> getAllFileItems() {
        return allFileItems;
    }

    /**
     * Inserisce un nuovo elemento file nel database.
     * Questa operazione viene delegata al {@link FileRepository}.
     *
     * @param fileItem L'oggetto {@link FileItem} da inserire.
     */
    public void insert(FileItem fileItem) {
        repository.insert(fileItem);
    }
    /**
     * Aggiorna un elemento file esistente nel database.
     * Questa operazione viene delegata al {@link FileRepository}.
     *
     * @param fileItem L'oggetto {@link FileItem} da aggiornare.
     */
    public void update(FileItem fileItem) {
        repository.update(fileItem);
    }
    /**
     * Elimina un elemento file dal database.
     * Questa operazione viene delegata al {@link FileRepository}.
     *
     * @param fileItem L'oggetto {@link FileItem} da eliminare.
     */
    public void delete(FileItem fileItem) {
        repository.delete(fileItem);
    }
    /**
     * Restituisce un {@link LiveData} contenente un singolo elemento file basato sul suo ID.
     * Utile per osservare i cambiamenti di un file specifico.
     *
     * @param fileItemId L'ID dell'elemento file da recuperare.
     * @return Un LiveData con l'oggetto {@link FileItem} corrispondente all'ID.
     */
    public LiveData<FileItem> getFileItemById(int fileItemId) {
        return repository.getFileItemById(fileItemId);
    }
}