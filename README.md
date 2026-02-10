# HookeXpert - License Generator & Manager

**Version:** 1.0  
**Developer:** [Houcine Latif/Hooke-Electronics)]

## 📋 Overview
The **License Generator** is an internal administration tool designed to manage access to the HookeXpert software. It allows you to:
* **Generate** secure, machine-bound license keys for clients.
* **Track** issued licenses and client history in a local database.
* **Validate** and decode existing license strings for troubleshooting.

---

## 🚀 Installation
This application is portable and does not require a standard installer.

1.  Locate the application folder (e.g., `LicenseGenerator`).
2.  Double-click **`LicenseGenerator.exe`** to launch.
3.  *(Optional)* Right-click the executable and select "Create Shortcut" to place a link on your desktop.

---

## 📖 Usage Guide

### 1. Generating a New License
Use this workflow when a client purchases HookeXpert or requests a trial.

1.  **Request Machine ID:** Ask the client to run HookeXpert on their computer. The activation screen will display a **32-character Machine ID** (e.g., `5A8F-....`). Ask them to copy and send this to you.
2.  Open the **"Générer Licence"** (Generate) tab.
3.  **Client Name:** Enter the client's name or company (e.g., "Acme Corp"). *This is required for the database history.*
4.  **Machine ID:** Paste the ID provided by the client.
5.  **Duration:** Select a validity period (30 days, 1 year, or specific day count).
6.  Click **"Générer la Clé"**.
7.  Click **"Copier"** and send the generated text string to the client via email.

### 2. Managing License History
The application automatically saves every key you generate to a local SQLite database (`licenses.db`).

1.  Open the **"Historique"** (History) tab.
2.  You will see a table of all issued licenses.
3.  **Search:** Use the search bar to find a specific client or Machine ID.
4.  **Recover Key:** If a client loses their key, right-click their row in the table and select **"Copier la clé complète"** to retrieve it.

### 3. Validating & Troubleshooting
If a client reports that their key is not working:

1.  Open the **"Valider / Décoder"** tab.
2.  Paste the license key string into the text box.
3.  *(Optional)* Paste the client's Machine ID into the secondary field to check if the key belongs to their specific computer.
4.  Click **"Vérifier la Clé"**.
    * **✅ GREEN:** The key is valid, the dates are correct, and (if provided) the machine matches.
    * **❌ RED:** The key is expired, corrupted, or generated for a different computer.

### 4. Testing on Your Machine
To test the licensing system on your own computer:
1.  Go to the **"Mon ID Machine"** tab.
2.  Copy your own ID.
3.  Go to the Generate tab and create a license using your ID.
4.  Use this key to unlock your local copy of HookeXpert.

---

## ⚠️ Important Notes
* **Database File:** The application creates a file named `licenses.db` in the same folder as the executable. **Do not delete this file**, or you will lose your client history. Back it up regularly.
* **Security:** This tool contains the private logic to generate keys. **Do not distribute this executable to clients.** Only send them the HookeXpert app and the generated key string.

## 🛠 Support
For technical issues with the generator:
* Ensure you have read/write permissions in the application folder (so the database can be saved).
* If the app fails to launch, check `error.log` if available.