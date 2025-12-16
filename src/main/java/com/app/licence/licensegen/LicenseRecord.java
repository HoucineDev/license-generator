package com.app.licence.licensegen;

import java.time.LocalDate;

/**
 * Represents a single license entry in the history database.
 */
public class LicenseRecord {
    private int id;
    private String clientName;
    private String machineId;
    private String licenseKey;
    private LocalDate generationDate;
    private LocalDate expirationDate;
    private String status; // "ACTIVE", "EXPIRED", "REVOKED"

    public LicenseRecord(int id, String clientName, String machineId, String licenseKey,
                         LocalDate generationDate, LocalDate expirationDate, String status) {
        this.id = id;
        this.clientName = clientName;
        this.machineId = machineId;
        this.licenseKey = licenseKey;
        this.generationDate = generationDate;
        this.expirationDate = expirationDate;
        this.status = status;
    }

    // Getters are required for JavaFX TableView PropertyValueFactory
    public int getId() { return id; }
    public String getClientName() { return clientName; }
    public String getMachineId() { return machineId; }
    public String getLicenseKey() { return licenseKey; }
    public LocalDate getGenerationDate() { return generationDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public String getStatus() { return status; }
}