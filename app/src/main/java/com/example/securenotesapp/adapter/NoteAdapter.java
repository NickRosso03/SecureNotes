package com.example.securenotesapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securenotesapp.R;
import com.example.securenotesapp.model.Note;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adattatore per RecyclerView utilizzato per visualizzare un elenco di oggetti {@link com.example.securenotesapp.model.Note}.
 * Gestisce la creazione e il binding delle ViewHolder per le note, e fornisce un'interfaccia per la gestione dei click sugli elementi.
 */
public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    // Inizializza la lista qui. Questo funge da costruttore predefinito.
    private List<Note> notes;
    private OnItemClickListener listener;

    /**
     * Costruttore per NoteAdapter che accetta una lista iniziale di note.
     *
     * @param initialNotes La lista iniziale di oggetti {@link com.example.securenotesapp.model.Note} da visualizzare.
     */
     public NoteAdapter(List<Note> initialNotes) {
         this.notes.addAll(initialNotes); // Aggiungi gli elementi alla lista esistente
     }

    /**
     * Costruttore predefinito per NoteAdapter. Inizializza la lista delle note come un nuovo ArrayList.
     */
     public NoteAdapter() {
         this.notes = new ArrayList<>();
     }

    /**
     * Aggiorna il set di dati dell'adattatore con una nuova lista di note e notifica il RecyclerView del cambiamento.
     *
     * @param notes La nuova lista di oggetti {@link com.example.securenotesapp.model.Note} da visualizzare.
     */
    public void setNotes(List<Note> notes) {
        this.notes = notes;
        notifyDataSetChanged(); // Notifica al RecyclerView che i dati sono cambiati
    }

    /**
     * Chiamato quando RecyclerView ha bisogno di una nuova {@link NoteViewHolder} del tipo dato per rappresentare un elemento.
     *
     * @param parent Il ViewGroup in cui verrà aggiunta la nuova View dopo che è stata legata a una posizione dell'adattatore.
     * @param viewType Il tipo di vista della nuova View.
     * @return Una nuova NoteViewHolder che contiene la View per ogni elemento nota.
     */
    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.note_item, parent, false);
        return new NoteViewHolder(itemView);
    }
    /**
     * Chiamato da RecyclerView per visualizzare i dati nella posizione specificata.
     * Questo metodo aggiorna i contenuti della {@link NoteViewHolder#itemView} per riflettere l'elemento alla data posizione.
     *
     * @param holder Il ViewHolder che deve essere aggiornato per rappresentare il contenuto dell'elemento
     * alla data `position` nel set di dati.
     * @param position La posizione dell'elemento all'interno del set di dati dell'adattatore.
     */
    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note currentNote = notes.get(position);
        holder.textViewTitle.setText(currentNote.getTitle());
        holder.textViewContentPreview.setText(currentNote.getContent());

        // Formatta il timestamp per una migliore leggibilità
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String formattedTimestamp = sdf.format(new Date(currentNote.getTimestamp()));
        holder.textViewTimestamp.setText(formattedTimestamp);
    }
    /**
     * Restituisce il numero totale di elementi nel set di dati in possesso dell'adattatore.
     *
     * @return Il numero totale di elementi in questo adattatore.
     */
    @Override
    public int getItemCount() {
        return notes.size();
    }

    /**
     * ViewHolder interno per rappresentare ogni elemento della lista (nota) nella RecyclerView.
     * Ogni istanza di NoteViewHolder gestisce il layout di un singolo elemento e i suoi componenti.
     */
    class NoteViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewTitle;
        private TextView textViewContentPreview;
        private TextView textViewTimestamp;
        /**
         * Costruttore per NoteViewHolder.
         *
         * @param itemView La View dell'elemento del RecyclerView.
         */
        public NoteViewHolder(View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.text_view_note_title);
            textViewContentPreview = itemView.findViewById(R.id.text_view_note_content_preview);
            textViewTimestamp = itemView.findViewById(R.id.text_view_note_timestamp);

            // Imposta un OnClickListener per l'intera View dell'elemento
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    // Assicura che il listener non sia null e la posizione sia valida
                    if (listener != null && position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(notes.get(position));
                    }
                }
            });
        }
    }

    /**
     * Interfaccia per definire un callback per i click sugli elementi della RecyclerView.
     * Questo permette all'Activity o al Fragment di reagire ai click sugli elementi.
     */
    public interface OnItemClickListener {
        /**
         * Chiamato quando un elemento della lista viene cliccato.
         * @param note L'oggetto {@link com.example.securenotesapp.model.Note} che è stato cliccato.
         */
        void onItemClick(Note note);
    }
    /**
     * Imposta il listener per i click sugli elementi.
     *
     * @param listener L'implementazione di {@link OnItemClickListener} da usare.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Metodo per ottenere una nota in una data posizione.
     * Utile per operazioni come swipe-to-delete.
     *
     * @param position La posizione dell'elemento da recuperare.
     * @return L'oggetto {@link com.example.securenotesapp.model.Note} alla posizione specificata.
     */
    public Note getNoteAt(int position) {
        return notes.get(position);
    }
}