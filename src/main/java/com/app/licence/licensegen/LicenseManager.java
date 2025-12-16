package com.app.licence.licensegen;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Base64;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Central license management system for HookeXpert.
 * Combines hardware fingerprinting, multi-storage trial tracking, and license validation.
 *
 * WORKFLOW:
 * 1. Client runs app → sees trial countdown or expiration dialog
 * 2. Client copies Machine ID and sends to developer
 * 3. Developer uses LicenseGenerator to create license key
 * 4. Client enters license key → App validates and activates
 */
public class LicenseManager {

    private static final Logger logger = Logger.getLogger(LicenseManager.class.getName());

    // IMPORTANT: Keep this secret and ONLY in your code, not shared with clients
    private static final String SECRET_SALT = "HookeXpert_License_Salt_2025!@#$%^&*";

    // Preferences key for storing license
    private static final String LICENSE_PREF_KEY = "license_key";

    private final MultiStorageTrialManager trialManager;
    private final String machineId;
    private final Preferences prefs;

    public LicenseManager() {
        this.trialManager = new MultiStorageTrialManager();
        this.machineId = HardwareFingerprint.generateMachineId();
        this.prefs = Preferences.userRoot().node("/hookexpert/license");
        logger.info("License Manager initialized. Machine ID: " + machineId);
    }

    /**
     * Get this machine's unique identifier.
     * Client sends this to you to generate a license.
     *
     * @return 32-character hex string unique to this machine
     */
    public String getMachineId() {
        return machineId;
    }

    /**
     * Check if a license key is valid for this specific machine.
     *
     * @param licenseKey The license key to validate
     * @return true if valid and not expired, false otherwise
     */
    public boolean validateLicenseKey(String licenseKey) {
        if (licenseKey == null || licenseKey.trim().isEmpty()) {
            return false;
        }

        try {
            // License format: BASE64(machineId|expirationDate|signature)
            String decoded = new String(Base64.getDecoder().decode(licenseKey.trim()));
            String[] parts = decoded.split("\\|");

            if (parts.length != 3) {
                logger.warning("Invalid license format: wrong number of parts");
                return false;
            }

            String licensedMachineId = parts[0];
            LocalDate expirationDate = LocalDate.parse(parts[1]);
            String signature = parts[2];

            // Check machine ID matches
            if (!licensedMachineId.equals(this.machineId)) {
                logger.warning("License machine ID mismatch");
                return false;
            }

            // Check not expired
            if (LocalDate.now().isAfter(expirationDate)) {
                logger.warning("License has expired");
                return false;
            }

            // Verify signature
            String expectedSignature = generateSignature(licensedMachineId, expirationDate);
            if (!signature.equals(expectedSignature)) {
                logger.warning("License signature mismatch");
                return false;
            }

            logger.info("License validated successfully. Expires: " + expirationDate);
            return true;

        } catch (Exception e) {
            logger.warning("License validation error: " + e.getMessage());
            return false;
        }
    }

    /**
     * DEVELOPER TOOL: Generate a license key for a client's machine.
     * Run this on YOUR machine with the client's machine ID.
     *
     * @param clientMachineId The client's machine ID (they send this to you)
     * @param expirationDate When the license should expire
     * @return License key string to send to client
     */
    public static String generateLicenseKey(String clientMachineId, LocalDate expirationDate) {
        String signature = generateSignature(clientMachineId, expirationDate);
        String raw = clientMachineId + "|" + expirationDate.toString() + "|" + signature;
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }

    /**
     * Generate signature for license validation.
     */
    private static String generateSignature(String machineId, LocalDate expiration) {
        String data = machineId + expiration.toString() + SECRET_SALT;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes());
            StringBuilder hex = new StringBuilder();
            // Use first 16 bytes (32 hex chars) for signature
            for (int i = 0; i < 16; i++) {
                hex.append(String.format("%02X", hash[i]));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    /**
     * Get stored license key from preferences.
     *
     * @return Stored license key or null if none
     */
    public String getStoredLicenseKey() {
        return prefs.get(LICENSE_PREF_KEY, null);
    }

    /**
     * Store a license key in preferences.
     *
     * @param licenseKey The license key to store
     */
    public void storeLicenseKey(String licenseKey) {
        prefs.put(LICENSE_PREF_KEY, licenseKey);
        try {
            prefs.flush();
        } catch (Exception e) {
            logger.warning("Could not flush license preferences: " + e.getMessage());
        }
    }

    /**
     * Clear stored license key.
     */
    public void clearStoredLicense() {
        prefs.remove(LICENSE_PREF_KEY);
    }

    /**
     * Check overall license status: licensed, trial, or expired.
     *
     * @return LicenseStatus with type, days remaining, and message
     */
    public LicenseStatus checkLicense() {
        // First check if there's a valid stored license key
        String storedLicense = getStoredLicenseKey();
        if (storedLicense != null && !storedLicense.isEmpty()) {
            if (validateLicenseKey(storedLicense)) {
                // Extract expiration date for display
                try {
                    String decoded = new String(Base64.getDecoder().decode(storedLicense));
                    String[] parts = decoded.split("\\|");
                    LocalDate expDate = LocalDate.parse(parts[1]);
                    long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expDate);
                    return new LicenseStatus(LicenseType.LICENSED, daysUntilExpiry,
                            "Version sous licence (expire le " + expDate + ")");
                } catch (Exception e) {
                    return new LicenseStatus(LicenseType.LICENSED, -1, "Version sous licence");
                }
            } else {
                // License invalid or expired - clear it
                clearStoredLicense();
            }
        }

        // Fall back to trial
        MultiStorageTrialManager.TrialStatus trialStatus = trialManager.checkTrial();

        if (trialStatus.isValid()) {
            return new LicenseStatus(LicenseType.TRIAL, trialStatus.getDaysRemaining(),
                    "Essai: " + trialStatus.getDaysRemaining() + " jour(s) restant(s)");
        }

        return new LicenseStatus(LicenseType.EXPIRED, 0, "Période d'essai expirée - Contactez le développeur");
    }

    /**
     * Activate a license key.
     *
     * @param licenseKey The license key to activate
     * @return true if activation successful, false otherwise
     */
    public boolean activateLicense(String licenseKey) {
        if (validateLicenseKey(licenseKey)) {
            storeLicenseKey(licenseKey);
            logger.info("License activated successfully");
            return true;
        }
        return false;
    }

    // ==================== STATUS CLASSES ====================

    /**
     * Types of license status.
     */
    public enum LicenseType {
        LICENSED,   // Full license active
        TRIAL,      // Trial period active
        EXPIRED     // Trial expired, no valid license
    }

    /**
     * Complete license status information.
     */
    public static class LicenseStatus {
        private final LicenseType type;
        private final long daysRemaining;
        private final String message;

        public LicenseStatus(LicenseType type, long daysRemaining, String message) {
            this.type = type;
            this.daysRemaining = daysRemaining;
            this.message = message;
        }

        public LicenseType getType() { return type; }
        public long getDaysRemaining() { return daysRemaining; }
        public String getMessage() { return message; }

        public boolean isUsable() {
            return type == LicenseType.LICENSED || type == LicenseType.TRIAL;
        }
    }
}

