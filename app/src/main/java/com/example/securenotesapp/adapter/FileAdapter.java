package com.example.securenotesapp.adapter;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securenotesapp.R;
import com.example.securenotesapp.model.FileItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
/**
 * Adattatore per RecyclerView utilizzato per visualizzare un elenco di oggetti {@link com.example.securenotesapp.model.FileItem}.
 * Gestisce la creazione e il binding delle ViewHolder, e fornisce un'interfaccia per la gestione dei click sugli elementi.
 */
public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileHolder> {

    private List<FileItem> fileItems = new ArrayList<>();
    private OnItemClickListener listener;
    private Context context;
    /**
     * Costruttore per FileAdapter.
     *
     * @param context Il contesto dell'applicazione, utilizzato per formattare le dimensioni dei file.
     */
    public FileAdapter(Context context) {
        this.context = context;
    }

    /**
     * Chiamato quando RecyclerView ha bisogno di una nuova {@link FileHolder} del tipo dato per rappresentare un elemento.
     *
     * @param parent Il ViewGroup in cui verrà aggiunta la nuova View dopo che è stata legata a una posizione dell'adattatore.
     * @param viewType Il tipo di vista della nuova View.
     * @return Una nuova FileHolder che contiene la View per ogni elemento.
     */
    @NonNull
    @Override
    public FileHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.file_item, parent, false);
        return new FileHolder(itemView);
    }
    /**
     * Chiamato da RecyclerView per visualizzare i dati nella posizione specificata.
     * Questo metodo aggiorna i contenuti della {@link FileHolder#itemView} per riflettere l'elemento alla data posizione.
     *
     * @param holder Il ViewHolder che deve essere aggiornato per rappresentare il contenuto dell'elemento
     * alla data `position` nel set di dati.
     * @param position La posizione dell'elemento all'interno del set di dati dell'adattatore.
     */
    @Override
    public void onBindViewHolder(@NonNull FileHolder holder, int position) {
        FileItem currentFile = fileItems.get(position);
        holder.fileNameTextView.setText(currentFile.getOriginalFileName());
        // Formatta la dimensione del file in un formato leggibile
        String sizeFormatted = Formatter.formatFileSize(context, currentFile.getFileSize());
        holder.fileSizeTextView.setText(sizeFormatted);

        // Formatta il timestamp del file in una data leggibile
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.fileDateTextView.setText(sdf.format(currentFile.getTimestamp()));
    }
    /**
     * Restituisce il numero totale di elementi nel set di dati in possesso dell'adattatore.
     *
     * @return Il numero totale di elementi in questo adattatore.
     */
    @Override
    public int getItemCount() {
        return fileItems.size();
    }
    /**
     * Aggiorna il set di dati dell'adattatore con una nuova lista di elementi file e notifica il RecyclerView del cambiamento.
     *
     * @param fileItems La nuova lista di oggetti {@link FileItem} da visualizzare.
     */
    public void setFileItems(List<FileItem> fileItems) {
        this.fileItems = fileItems;
        notifyDataSetChanged();
    }
    /**
     * Restituisce l'elemento {@link FileItem} alla posizione specificata.
     *
     * @param position La posizione dell'elemento da recuperare.
     * @return L'oggetto FileItem alla posizione specificata.
     */
    public FileItem getFileItemAt(int position) {
        return fileItems.get(position);
    }
    /**
     * Rappresenta un singolo elemento della lista (file) nella RecyclerView.
     * Ogni istanza di FileHolder gestisce il layout di un singolo elemento e i suoi componenti.
     */
    class FileHolder extends RecyclerView.ViewHolder {
        private TextView fileNameTextView;
        private TextView fileSizeTextView;
        private TextView fileDateTextView;
        /**
         * Costruttore per FileHolder.
         *
         * @param itemView La View dell'elemento del RecyclerView.
         */
        public FileHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.text_view_file_name);
            fileSizeTextView = itemView.findViewById(R.id.text_view_file_size);
            fileDateTextView = itemView.findViewById(R.id.text_view_file_date);

            // Imposta un OnClickListener per l'intera View dell'elemento
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (listener != null && position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(fileItems.get(position));
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
        void onItemClick(FileItem fileItem);
    }
    /**
     * Imposta il listener per i click sugli elementi.
     *
     * @param listener L'implementazione di {@link OnItemClickListener} da usare.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}