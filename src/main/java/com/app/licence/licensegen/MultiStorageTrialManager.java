package com.app.licence.licensegen;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages trial period with multiple storage locations for robustness.
 * Stores trial start date in several places to prevent simple deletion/bypass.
 */
public class MultiStorageTrialManager {

    private static final Logger logger = Logger.getLogger(MultiStorageTrialManager.class.getName());

    // Configuration - CUSTOMIZE THESE VALUES
    private static final int TRIAL_DAYS = 14;  // Number of trial days
    private static final String SECRET_KEY = "HookeXpert2025!SecretKey#@$";
    private static final String APP_NAME = "HookeXpert";

    // Multiple storage providers
    private final List<StorageProvider> storageProviders;

    public MultiStorageTrialManager() {
        storageProviders = Arrays.asList(
                new PreferencesStorage(),
                new HiddenFileStorage(),
                new TempFolderStorage(),
                new AppDataStorage()
        );
    }

    /**
     * Check the trial status.
     * Reads from all storage locations and uses the earliest date found.
     * Repairs any missing storage locations by writing to all.
     *
     * @return TrialStatus with validity, days remaining, and message
     */
    public TrialStatus checkTrial() {
        // Collect all stored dates from different locations
        List<LocalDate> storedDates = new ArrayList<>();

        for (StorageProvider provider : storageProviders) {
            try {
                LocalDate date = provider.readDate();
                if (date != null) {
                    storedDates.add(date);
                    logger.fine("Found trial date in " + provider.getClass().getSimpleName() + ": " + date);
                }
            } catch (Exception e) {
                logger.log(Level.FINE, "Could not read from " + provider.getClass().getSimpleName(), e);
            }
        }

        LocalDate firstLaunchDate;

        if (storedDates.isEmpty()) {
            // First launch ever
            firstLaunchDate = LocalDate.now();
            logger.info("First launch detected, starting trial period");
        } else {
            // Use the EARLIEST date (prevents reinstall trick)
            firstLaunchDate = Collections.min(storedDates);
            logger.fine("Using earliest stored date: " + firstLaunchDate);
        }

        // Rewrite to ALL storage locations (repair deleted ones)
        int successfulWrites = 0;
        for (StorageProvider provider : storageProviders) {
            try {
                provider.writeDate(firstLaunchDate);
                successfulWrites++;
            } catch (Exception e) {
                logger.log(Level.FINE, "Could not write to " + provider.getClass().getSimpleName(), e);
            }
        }
        logger.fine("Trial date written to " + successfulWrites + "/" + storageProviders.size() + " storage locations");

        // Calculate trial status
        long daysUsed = ChronoUnit.DAYS.between(firstLaunchDate, LocalDate.now());
        long daysRemaining = TRIAL_DAYS - daysUsed;

        // Check for system clock manipulation (date set backwards)
        if (daysUsed < 0) {
            logger.warning("System clock manipulation detected");
            return new TrialStatus(false, 0, "Manipulation de l'horloge système détectée");
        }

        if (daysRemaining <= 0) {
            return new TrialStatus(false, 0, "Période d'essai expirée");
        }

        return new TrialStatus(true, daysRemaining, "Essai actif");
    }

    /**
     * Get the configured trial duration.
     */
    public int getTrialDays() {
        return TRIAL_DAYS;
    }

    // ==================== STORAGE PROVIDERS ====================

    /**
     * Interface for different storage backends.
     */
    interface StorageProvider {
        LocalDate readDate() throws Exception;
        void writeDate(LocalDate date) throws Exception;
    }

    /**
     * Storage using Java Preferences API (Windows Registry / Linux ~/.java/.userPrefs).
     */
    class PreferencesStorage implements StorageProvider {
        private final Preferences prefs;

        PreferencesStorage() {
            prefs = Preferences.userRoot().node("/" + APP_NAME.toLowerCase() + "/license");
        }

        @Override
        public LocalDate readDate() {
            String encoded = prefs.get("init_ts", null);
            if (encoded == null) return null;
            return LocalDate.parse(decrypt(encoded));
        }

        @Override
        public void writeDate(LocalDate date) {
            prefs.put("init_ts", encrypt(date.toString()));
            try {
                prefs.flush();
            } catch (Exception e) {
                // Best effort
            }
        }
    }

    /**
     * Storage using a hidden file in user's home directory.
     */
    class HiddenFileStorage implements StorageProvider {
        private final Path filePath;

        HiddenFileStorage() {
            String os = System.getProperty("os.name").toLowerCase();
            String home = System.getProperty("user.home");

            if (os.contains("win")) {
                filePath = Paths.get(home, "." + APP_NAME.toLowerCase() + ".dat");
            } else {
                filePath = Paths.get(home, "." + APP_NAME.toLowerCase(), ".config");
            }
        }

        @Override
        public LocalDate readDate() throws Exception {
            if (!Files.exists(filePath)) return null;
            String content = Files.readString(filePath);
            return LocalDate.parse(decrypt(content.trim()));
        }

        @Override
        public void writeDate(LocalDate date) throws Exception {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, encrypt(date.toString()));

            // Make hidden on Windows
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                try {
                    Files.setAttribute(filePath, "dos:hidden", true);
                } catch (Exception e) {
                    // Best effort - may fail on some systems
                }
            }
        }
    }

    /**
     * Storage using a file in the system temp directory.
     */
    class TempFolderStorage implements StorageProvider {
        private final Path filePath;

        TempFolderStorage() {
            String tempDir = System.getProperty("java.io.tmpdir");
            String hash = hashString(APP_NAME + SECRET_KEY).substring(0, 12);
            filePath = Paths.get(tempDir, "." + hash + ".tmp");
        }

        @Override
        public LocalDate readDate() throws Exception {
            if (!Files.exists(filePath)) return null;
            String content = Files.readString(filePath);
            return LocalDate.parse(decrypt(content.trim()));
        }

        @Override
        public void writeDate(LocalDate date) throws Exception {
            Files.writeString(filePath, encrypt(date.toString()));
        }
    }

    /**
     * Storage using the OS-specific application data folder.
     * - Windows: %APPDATA%\HookeXpert
     * - macOS: ~/Library/Application Support/HookeXpert
     * - Linux: ~/.config/hookexpert
     */
    class AppDataStorage implements StorageProvider {
        private final Path filePath;

        AppDataStorage() {
            String os = System.getProperty("os.name").toLowerCase();
            String home = System.getProperty("user.home");

            if (os.contains("win")) {
                String appData = System.getenv("APPDATA");
                if (appData == null) {
                    appData = Paths.get(home, "AppData", "Roaming").toString();
                }
                filePath = Paths.get(appData, APP_NAME, "license.dat");
            } else if (os.contains("mac")) {
                filePath = Paths.get(home, "Library", "Application Support", APP_NAME, ".license");
            } else {
                // Linux and others
                filePath = Paths.get(home, ".config", APP_NAME.toLowerCase(), "license.dat");
            }
        }

        @Override
        public LocalDate readDate() throws Exception {
            if (!Files.exists(filePath)) return null;
            String content = Files.readString(filePath);
            return LocalDate.parse(decrypt(content.trim()));
        }

        @Override
        public void writeDate(LocalDate date) throws Exception {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, encrypt(date.toString()));
        }
    }

    // ==================== ENCRYPTION HELPERS ====================

    /**
     * Simple XOR-based encryption with Base64 encoding.
     * Not cryptographically secure, but sufficient to prevent casual tampering.
     */
    private String encrypt(String data) {
        try {
            byte[] dataBytes = data.getBytes();
            byte[] keyBytes = SECRET_KEY.getBytes();
            byte[] result = new byte[dataBytes.length];

            for (int i = 0; i < dataBytes.length; i++) {
                result[i] = (byte) (dataBytes[i] ^ keyBytes[i % keyBytes.length]);
            }
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            return data;
        }
    }

    /**
     * Decrypts data encrypted with encrypt().
     */
    private String decrypt(String encoded) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            byte[] keyBytes = SECRET_KEY.getBytes();
            byte[] result = new byte[decoded.length];

            for (int i = 0; i < decoded.length; i++) {
                result[i] = (byte) (decoded[i] ^ keyBytes[i % keyBytes.length]);
            }
            return new String(result);
        } catch (Exception e) {
            throw new RuntimeException("Corrupted trial data");
        }
    }

    /**
     * Creates a SHA-256 hash of the input string.
     */
    private String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return input;
        }
    }

    // ==================== STATUS CLASS ====================

    /**
     * Represents the current trial status.
     */
    public static class TrialStatus {
        private final boolean valid;
        private final long daysRemaining;
        private final String message;

        public TrialStatus(boolean valid, long daysRemaining, String message) {
            this.valid = valid;
            this.daysRemaining = daysRemaining;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public long getDaysRemaining() { return daysRemaining; }
        public String getMessage() { return message; }
    }
}

