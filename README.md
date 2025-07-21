# SecureNotes - App Android per Note e File Sensibili con Sicurezza Avanzata

**SecureNotes** è un'applicazione Android progettata per offrire agli utenti un ambiente sicuro per la creazione di note personali e l'archiviazione di file sensibili.  
L'obiettivo principale è garantire la massima protezione dei dati attraverso robuste funzionalità di sicurezza e un'architettura moderna.

---

## ✨ Caratteristiche Principali

- **Autenticazione Sicura**  
  Sblocco tramite biometria (impronta, volto) o PIN di fallback, richiesto all'avvio, dopo timeout di inattività e prima di accedere a contenuti sensibili.

- **Crittografia Dati Locali End-to-End**  
  Tutte le note e i file sono crittografati con AES/GCM. Il database Room è ulteriormente protetto da SQLCipher (crittografia a livello di file). Le chiavi sono gestite in modo sicuro tramite Android Keystore.

- **Archivio Sicuro di File**  
  Caricamento, archiviazione e visualizzazione di documenti (PDF, immagini) criptati internamente con `EncryptedFile` di Jetpack Security.

- **Timeout di Sessione Automatico**  
  Blocco automatico dell'app dopo un periodo di inattività configurabile.

- **Backup Criptato**  
  Funzionalità di esportazione/importazione di backup `.zip` protetti da password e crittografati con AES. I backup automatici di Android sono disabilitati per il massimo controllo sulla privacy.

- **Offuscamento del Codice (R8)**  
  Protezione contro il reverse engineering tramite offuscamento di classi, metodi e campi.

---

## 🛠 Tecnologie Utilizzate

- **Linguaggio:** Java (versione 11)  
- **IDE:** Android Studio  
- **Architettura:** MVVM (Model-View-ViewModel) con Repository Pattern  

### 📦 Componenti Android Jetpack

- `androidx.biometric` – Autenticazione biometrica  
- `androidx.security.crypto` – EncryptedSharedPreferences e EncryptedFile  
- `androidx.room` – Persistenza del database SQLite  
- `androidx.work` – Operazioni in background  
- `androidx.documentfile` – Interazione sicura con file esterni  

### 🔐 Altri Strumenti

- **Crittografia Database:** `net.zetetic:android-database-sqlcipher`  
- **Serializzazione JSON:** `com.google.code.gson`

---

## 🧩 Architettura

Il progetto segue il pattern **MVVM**, con una chiara separazione delle responsabilità:

- **Model**  
  Contiene le entità dati (`Note`, `FileItem`) e i DAO per il database.

- **View**  
  Composta da `Activity` e `Fragment` che gestiscono l'interfaccia utente.

- **ViewModel**  
  Intermediario tra Model e View, espone dati osservabili e gestisce la logica di presentazione.

- **Repository**  
  Astrae l'accesso ai dati, fornendo un'API pulita ai ViewModel e gestendo le operazioni sul database in thread separati.

---

## 🛡️ Sicurezza al Centro

SecureNotes è progettata con un approccio *privacy-first*, senza alcuna dipendenza da servizi cloud o tracciamenti. Tutti i dati restano sul dispositivo, completamente crittografati e sotto il controllo dell'utente.

---
