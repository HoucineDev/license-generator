package com.app.licence.licensegen;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite database for storing license generation history.
 *
 * FIXED: Corrected SQL syntax error (missing comma between columns)
 */
public class LicenseDatabase {

    private static final String DB_URL = "jdbc:sqlite:licenses.db";

    public LicenseDatabase() {
        initialize();
    }

    private void initialize() {
        // FIXED: Added missing comma between "status TEXT" and "role TEXT"
        String sql = "CREATE TABLE IF NOT EXISTS licenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "client_name TEXT," +
                "machine_id TEXT," +
                "license_key TEXT," +
                "gen_date TEXT," +
                "exp_date TEXT," +
                "status TEXT," +  // <-- FIXED: Added comma here
                "role TEXT" +
                ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);

            // Migration for existing DBs - add role column if missing
            try {
                stmt.execute("ALTER TABLE licenses ADD COLUMN role TEXT DEFAULT 'USER'");
            } catch (SQLException ignored) {
                // Column likely already exists
            }

            // Migration: add status column if missing (for older DBs)
            try {
                stmt.execute("ALTER TABLE licenses ADD COLUMN status TEXT DEFAULT 'ACTIVE'");
            } catch (SQLException ignored) {
                // Column likely already exists
            }

        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Add a new license record to the database.
     *
     * @param clientName  Client name or reference
     * @param machineId   Client's machine ID
     * @param key         Generated license key
     * @param expDate     Expiration date
     * @param status      License status (ACTIVE, ESSAI, EXPIRED, REVOKED)
     * @param role        License role (USER, ADMIN)
     * @return The created LicenseRecord, or null on failure
     */
    public LicenseRecord addLicense(String clientName, String machineId, String key,
                                    LocalDate expDate, String status, String role) {
        String sql = "INSERT INTO licenses(client_name, machine_id, license_key, gen_date, exp_date, status, role) " +
                "VALUES(?,?,?,?,?,?,?)";
        LocalDate now = LocalDate.now();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, clientName);
            pstmt.setString(2, machineId);
            pstmt.setString(3, key);
            pstmt.setString(4, now.toString());
            pstmt.setString(5, expDate.toString());
            pstmt.setString(6, status);
            pstmt.setString(7, role);
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new LicenseRecord(
                            generatedKeys.getInt(1),
                            clientName,
                            machineId,
                            key,
                            now,
                            expDate,
                            status,
                            role
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding license: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get all licenses from the database, newest first.
     */
    public List<LicenseRecord> getAllLicenses() {
        List<LicenseRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM licenses ORDER BY id DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new LicenseRecord(
                        rs.getInt("id"),
                        rs.getString("client_name"),
                        rs.getString("machine_id"),
                        rs.getString("license_key"),
                        LocalDate.parse(rs.getString("gen_date")),
                        LocalDate.parse(rs.getString("exp_date")),
                        rs.getString("status"),
                        rs.getString("role")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching licenses: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Search licenses by client name or machine ID.
     */
    public List<LicenseRecord> search(String query) {
        List<LicenseRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM licenses WHERE client_name LIKE ? OR machine_id LIKE ? ORDER BY id DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + query + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                list.add(new LicenseRecord(
                        rs.getInt("id"),
                        rs.getString("client_name"),
                        rs.getString("machine_id"),
                        rs.getString("license_key"),
                        LocalDate.parse(rs.getString("gen_date")),
                        LocalDate.parse(rs.getString("exp_date")),
                        rs.getString("status"),
                        rs.getString("role")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error searching licenses: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Update the status of a license.
     *
     * @param licenseId The ID of the license to update
     * @param newStatus The new status (ACTIVE, EXPIRED, REVOKED)
     * @return true if update successful
     */
    public boolean updateStatus(int licenseId, String newStatus) {
        String sql = "UPDATE licenses SET status = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setInt(2, licenseId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating license status: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete a license record.
     *
     * @param licenseId The ID of the license to delete
     * @return true if deletion successful
     */
    public boolean deleteLicense(int licenseId) {
        String sql = "DELETE FROM licenses WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, licenseId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting license: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get a license by its key.
     */
    public LicenseRecord getLicenseByKey(String licenseKey) {
        String sql = "SELECT * FROM licenses WHERE license_key = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, licenseKey);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new LicenseRecord(
                        rs.getInt("id"),
                        rs.getString("client_name"),
                        rs.getString("machine_id"),
                        rs.getString("license_key"),
                        LocalDate.parse(rs.getString("gen_date")),
                        LocalDate.parse(rs.getString("exp_date")),
                        rs.getString("status"),
                        rs.getString("role")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching license by key: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}