package com.example.securenotesapp.utils;

import androidx.room.TypeConverter;
import java.util.Date;
/**
 * Classe utility che fornisce metodi di conversione per {@link java.util.Date}
 * e {@link java.lang.Long}. È utilizzata da Room Persistence Library
 * per convertire oggetti Date in un formato che può essere memorizzato nel database (Long timestamp)
 * e viceversa.
 *
 * Questo è necessario perché Room può salvare direttamente solo tipi primitivi o tipi
 * che implementano un convertitore di tipo.
 */
public class DateConverter {
    /**
     * Converte un oggetto {@link java.util.Date} in un {@link java.lang.Long} che rappresenta
     * il numero di millisecondi dal 1° gennaio 1970, 00:00:00 GMT (Epoch).
     * Questo metodo è annotato con @TypeConverter, indicando a Room come convertire
     * un oggetto Date prima di salvarlo nel database.
     *
     * @param date L'oggetto Date da convertire. Può essere null.
     * @return Il timestamp Long corrispondente, o null se l'input date è null.
     */
    @TypeConverter
    public static Long fromDate(Date date) {
        return date == null ? null : date.getTime();
    }
    /**
     * Converte un {@link java.lang.Long} (timestamp in millisecondi) in un oggetto
     * {@link java.util.Date}.
     * Questo metodo è annotato con @TypeConverter, indicando a Room come convertire
     * un Long letto dal database in un oggetto Date.
     *
     * @param timestamp Il timestamp Long da convertire. Può essere null.
     * @return L'oggetto Date corrispondente, o null se l'input timestamp è null.
     */
    @TypeConverter
    public static Date toDate(Long timestamp) {
        // Se il timestamp è null, restituisce null. Altrimenti, crea un nuovo oggetto Date dal timestamp.
        return timestamp == null ? null : new Date(timestamp);
    }
}