package com.app.licence.licensegen;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LicenseDatabase {

    private static final String DB_URL = "jdbc:sqlite:licenses.db";

    public LicenseDatabase() {
        initialize();
    }

    private void initialize() {
        String sql = "CREATE TABLE IF NOT EXISTS licenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "client_name TEXT," +
                "machine_id TEXT," +
                "license_key TEXT," +
                "gen_date TEXT," +
                "exp_date TEXT," +
                "status TEXT" +
                ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Update this method in LicenseDatabase.java
    public LicenseRecord addLicense(String clientName, String machineId, String key, LocalDate expDate) {
        String sql = "INSERT INTO licenses(client_name, machine_id, license_key, gen_date, exp_date, status) VALUES(?,?,?,?,?,?)";
        LocalDate now = LocalDate.now();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, clientName);
            pstmt.setString(2, machineId);
            pstmt.setString(3, key);
            pstmt.setString(4, now.toString());
            pstmt.setString(5, expDate.toString());
            pstmt.setString(6, "ACTIVE");
            pstmt.executeUpdate();

            // Get the generated ID to create a valid record object
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new LicenseRecord(generatedKeys.getInt(1), clientName, machineId, key, now, expDate, "ACTIVE");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

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
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Search method
    public List<LicenseRecord> search(String query) {
        List<LicenseRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM licenses WHERE client_name LIKE ? OR machine_id LIKE ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + query + "%");
            pstmt.setString(2, "%" + query + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                list.add(new LicenseRecord(
                        rs.getInt("id"),
                        rs.getString("client_name"),
                        rs.getString("machine_id"),
                        rs.getString("license_key"),
                        LocalDate.parse(rs.getString("gen_date")),
                        LocalDate.parse(rs.getString("exp_date")),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}