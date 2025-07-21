package com.example.securenotesapp.database; // Assicurati che il package sia questo (senza 's')

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.sqlcipher.database.SupportFactory;

import com.example.securenotesapp.dao.FileDao;
import com.example.securenotesapp.dao.NoteDao;
import com.example.securenotesapp.model.FileItem;
import com.example.securenotesapp.model.Note;
import com.example.securenotesapp.utils.DateConverter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * Classe astratta che rappresenta il database dell'applicazione SecureNotes.
 * Utilizza Room per la persistenza dei dati e SQLCipher per la crittografia end-to-end.
 * Questa classe è un Singleton per garantire una singola istanza del database in tutta l'applicazione.
 *
 * Le entità gestite sono {@link com.example.securenotesapp.model.Note} e {@link com.example.securenotesapp.model.FileItem}.
 *
 * La versione del database è 2 e gestisce le migrazioni.
 *
 * @see androidx.room.RoomDatabase
 * @see net.sqlcipher.database.SupportFactory
 * @see com.example.securenotesapp.dao.NoteDao
 * @see com.example.securenotesapp.dao.FileDao
 * @see com.example.securenotesapp.model.Note
 * @see com.example.securenotesapp.model.FileItem
 */
@Database(
        entities = {Note.class, FileItem.class},
        version = 2,
        exportSchema = true // prima era false
)
@TypeConverters({DateConverter.class}) // Assicurati che questa riga ci sia se usi DateConverter
public abstract class NoteDatabase extends RoomDatabase {
    /**
     * Ritorna l'istanza del Data Access Object (DAO) per le note.
     * @return Il {@link NoteDao} per interagire con le note.
     */
    public abstract NoteDao noteDao();
    /**
     * Ritorna l'istanza del Data Access Object (DAO) per gli elementi file.
     * @return Il {@link FileDao} per interagire con gli elementi file.
     */
    public abstract FileDao fileDao();

    private static volatile NoteDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    private static final String TAG = "NoteDatabase";


    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Aggiunge la colonna 'fileSize' alla tabella 'file_items'
            // NOTA: DEFAULT 0 è importante se la colonna non può essere NULL e le righe esistenti non hanno un valore
            database.execSQL("ALTER TABLE file_items ADD COLUMN fileSize INTEGER NOT NULL DEFAULT 0");
        }
    };

    /**
     * Restituisce l'istanza singola del database {@link NoteDatabase}.
     * Se l'istanza non esiste, ne crea una nuova, inizializzandola con SQLCipher
     * e la passphrase fornita. Applica le migrazioni necessarie.
     *
     * @param context Il contesto dell'applicazione.
     * @param passphrase La passphrase (chiave di crittografia) come array di byte.
     * @return L'istanza di {@link NoteDatabase}.
     */
    public static NoteDatabase getDatabase(final Context context, final byte[] passphrase) {
        if (INSTANCE == null) {
            synchronized (NoteDatabase.class) {
                if (INSTANCE == null) {
                    // Crea il factory per SQLCipher
                    SupportFactory factory = new SupportFactory(passphrase);

                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    NoteDatabase.class, "secure_notes_database.db") // Nome del database con .db
                            .openHelperFactory(factory)
                            // ***  MIGRAZIONE ***
                            .addMigrations(MIGRATION_1_2)
                            // .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
    /**
     * Chiude l'istanza del database se è aperta e rilascia il riferimento.
     * Questo è utile per la gestione del ciclo di vita e per prevenire memory leak.
     */
    public static void closeDatabase() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
            INSTANCE = null; // Rilascia l'istanza
            Log.d(TAG, "NoteDatabase closed and instance released.");
        }
    }
}