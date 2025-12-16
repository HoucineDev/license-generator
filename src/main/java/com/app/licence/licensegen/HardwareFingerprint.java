package com.app.licence.licensegen;

import java.io.*;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.*;

/**
 * Generates a unique hardware fingerprint based on multiple system identifiers.
 * Used for machine-bound licensing.
 */
public class HardwareFingerprint {

    /**
     * Generates a unique machine ID based on hardware characteristics.
     * Combines multiple identifiers for robustness.
     *
     * @return A 32-character uppercase hex string unique to this machine
     */
    public static String generateMachineId() {
        StringBuilder sb = new StringBuilder();

        // Collect multiple hardware identifiers
        sb.append(getMotherboardSerial());
        sb.append("|");
        sb.append(getCpuId());
        sb.append("|");
        sb.append(getMacAddress());
        sb.append("|");
        sb.append(getDiskSerial());
        sb.append("|");
        sb.append(getOsInfo());

        // Hash it to create a fixed-length fingerprint
        return hashSHA256(sb.toString()).substring(0, 32).toUpperCase();
    }

    /**
     * Gets the primary MAC address of the machine.
     */
    private static String getMacAddress() {
        try {
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                NetworkInterface network = networks.nextElement();
                byte[] mac = network.getHardwareAddress();

                if (mac != null && mac.length > 0 && !network.isLoopback() && !network.isVirtual()) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : mac) {
                        sb.append(String.format("%02X", b));
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            // Fallback silently
        }
        return "NO_MAC";
    }

    /**
     * Gets the CPU identifier (platform-specific).
     */
    private static String getCpuId() {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                return executeCommand("wmic cpu get ProcessorId");
            } else if (os.contains("linux")) {
                // Try multiple methods for Linux
                String result = executeCommand("cat /proc/cpuinfo | grep -m1 'model name' | cut -d: -f2");
                if (result.isEmpty()) {
                    result = executeCommand("lscpu | grep 'Model name' | cut -d: -f2");
                }
                return result.isEmpty() ? getLinuxMachineId() : result;
            } else if (os.contains("mac")) {
                return executeCommand("sysctl -n machdep.cpu.brand_string");
            }
        } catch (Exception e) {
            // Fallback silently
        }
        return "NO_CPU_ID";
    }

    /**
     * Gets the motherboard serial number (platform-specific).
     */
    private static String getMotherboardSerial() {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                return executeCommand("wmic baseboard get SerialNumber");
            } else if (os.contains("linux")) {
                // Try without sudo first (might work in some configurations)
                String result = executeCommand("cat /sys/class/dmi/id/board_serial 2>/dev/null");
                if (result.isEmpty()) {
                    result = getLinuxMachineId();
                }
                return result;
            } else if (os.contains("mac")) {
                return executeCommand("ioreg -rd1 -c IOPlatformExpertDevice | grep IOPlatformSerialNumber | cut -d'\"' -f4");
            }
        } catch (Exception e) {
            // Fallback silently
        }
        return "NO_MOBO_SERIAL";
    }

    /**
     * Gets the disk serial number (platform-specific).
     */
    private static String getDiskSerial() {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                return executeCommand("wmic diskdrive get SerialNumber");
            } else if (os.contains("linux")) {
                String result = executeCommand("lsblk -o SERIAL 2>/dev/null | head -2 | tail -1");
                if (result.isEmpty()) {
                    // Fallback to /etc/machine-id which is stable
                    result = getLinuxMachineId();
                }
                return result;
            } else if (os.contains("mac")) {
                return executeCommand("diskutil info disk0 | grep 'Volume UUID' | cut -d: -f2");
            }
        } catch (Exception e) {
            // Fallback silently
        }
        return "NO_DISK_SERIAL";
    }

    /**
     * Gets the Linux machine-id which is stable across reboots.
     */
    private static String getLinuxMachineId() {
        try {
            File machineIdFile = new File("/etc/machine-id");
            if (machineIdFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(machineIdFile))) {
                    String line = reader.readLine();
                    if (line != null && !line.isEmpty()) {
                        return line.trim();
                    }
                }
            }

            // Fallback to /var/lib/dbus/machine-id
            File dbusIdFile = new File("/var/lib/dbus/machine-id");
            if (dbusIdFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(dbusIdFile))) {
                    String line = reader.readLine();
                    if (line != null && !line.isEmpty()) {
                        return line.trim();
                    }
                }
            }
        } catch (Exception e) {
            // Fallback silently
        }
        return "NO_LINUX_MACHINE_ID";
    }

    /**
     * Gets basic OS and user information.
     */
    private static String getOsInfo() {
        return System.getProperty("os.name") +
                System.getProperty("os.arch") +
                System.getProperty("user.name");
    }

    /**
     * Executes a system command and returns the output.
     */
    private static String executeCommand(String command) {
        try {
            Process process;
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                process = Runtime.getRuntime().exec(new String[]{"cmd", "/c", command});
            } else {
                process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Filter out header lines from wmic commands
                if (!line.isEmpty() &&
                        !line.equalsIgnoreCase("serialnumber") &&
                        !line.equalsIgnoreCase("processorid")) {
                    result.append(line);
                }
            }

            process.waitFor();
            reader.close();
            return result.toString().trim();

        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Computes SHA-256 hash of the input string.
     */
    private static String hashSHA256(String input) {
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

    /**
     * Test method to display generated machine ID.
     */
    public static void main(String[] args) {
        System.out.println("=== Hardware Fingerprint Generator ===");
        System.out.println("Machine ID: " + generateMachineId());
        System.out.println("\nComponents:");
        System.out.println("  MAC Address: " + getMacAddress());
        System.out.println("  CPU ID: " + getCpuId());
        System.out.println("  Motherboard: " + getMotherboardSerial());
        System.out.println("  Disk Serial: " + getDiskSerial());
        System.out.println("  OS Info: " + getOsInfo());
    }
}

