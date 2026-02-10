package com.app.licence.licensegen;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * Generates a unique hardware fingerprint based on multiple system identifiers.
 * Used for machine-bound licensing.
 *
 * VERSION 4.0 - STABLE COMPONENTS ONLY + CACHED
 *
 * IMPORTANT: This algorithm MUST be IDENTICAL to the one in HookeX's
 * HardwareFingerprint.java to ensure license compatibility!
 *
 * Changes v4.0:
 * - REMOVED MAC address (volatile: changes with VPN, WiFi toggle, new adapters)
 * - REMOVED Disk Serial (volatile: changes on disk swap or firmware update)
 * - Keeps ONLY: Motherboard/BIOS + CPU ID + OS info (truly stable)
 * - Added caching (from HookeX v3.0) for performance
 * - Cache versioning to force regeneration on algorithm changes
 *
 * Changes v2.0:
 * - Removed user.name (was causing license loss on user switch)
 * - Added PowerShell fallbacks for Windows 11 (WMIC is deprecated)
 */
public class HardwareFingerprint {

    // Preferences node for caching the Machine ID
    private static final String PREFS_NODE = "/hookexpert/hardware";
    private static final String CACHED_ID_KEY = "cached_machine_id";
    // Version tag to detect algorithm changes and force cache regeneration
    private static final String CACHE_VERSION_KEY = "cache_version";
    private static final String CURRENT_CACHE_VERSION = "4.0";

    /**
     * Gets the Machine ID - uses CACHE for fast startup.
     * Only generates a new ID if cache is empty or version mismatches.
     *
     * @return A 32-character uppercase hex string unique to this machine
     */
    public static String generateMachineId() {
        // Try to get cached Machine ID first (FAST)
        try {
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
            String cachedVersion = prefs.get(CACHE_VERSION_KEY, "");
            String cachedId = prefs.get(CACHED_ID_KEY, null);

            // Only use cache if version matches (algorithm hasn't changed)
            if (CURRENT_CACHE_VERSION.equals(cachedVersion)
                    && cachedId != null && cachedId.length() == 32) {
                // Cache hit - return immediately (fast path)
                return cachedId;
            }
        } catch (Exception e) {
            // Cache read failed, continue to generate
        }

        // Cache miss or version mismatch - generate new Machine ID (slow path)
        String newId = generateNewMachineId();

        // Store in cache for next time (with version tag)
        try {
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
            prefs.put(CACHED_ID_KEY, newId);
            prefs.put(CACHE_VERSION_KEY, CURRENT_CACHE_VERSION);
            prefs.flush();
        } catch (Exception e) {
            // Cache write failed - not critical, will just be slower next time
        }

        return newId;
    }

    /**
     * Forces regeneration of Machine ID and updates cache.
     * Use this if hardware has changed and you need a new ID.
     */
    public static String regenerateMachineId() {
        String newId = generateNewMachineId();

        try {
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
            prefs.put(CACHED_ID_KEY, newId);
            prefs.put(CACHE_VERSION_KEY, CURRENT_CACHE_VERSION);
            prefs.flush();
        } catch (Exception e) {
            // Ignore
        }

        return newId;
    }

    /**
     * Clears the cached Machine ID.
     * Next call to generateMachineId() will regenerate it.
     */
    public static void clearCache() {
        try {
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
            prefs.remove(CACHED_ID_KEY);
            prefs.remove(CACHE_VERSION_KEY);
            prefs.flush();
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Internal: Generates a new Machine ID from hardware (SLOW - takes ~2-3
     * seconds)
     *
     * IMPORTANT: This algorithm MUST match HookeX's HardwareFingerprint exactly!
     * v4.0: Only Motherboard + CPU + OS (no volatile MAC/Disk)
     */
    private static String generateNewMachineId() {
        StringBuilder sb = new StringBuilder();

        // v4.0: Only STABLE hardware identifiers
        // REMOVED: MAC address (volatile - changes with VPN, WiFi toggle, new adapters)
        // REMOVED: Disk Serial (volatile - changes on disk swap or firmware update)
        sb.append(getMotherboardSerial());
        sb.append("|");
        sb.append(getCpuId());
        sb.append("|");
        sb.append(getStableOsInfo());

        // Hash it to create a fixed-length fingerprint
        return hashSHA256(sb.toString()).substring(0, 32).toUpperCase();
    }

    /**
     * Gets the CPU identifier (platform-specific).
     * Windows 11: Uses PowerShell as primary, WMIC as fallback
     */
    private static String getCpuId() {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                // Try PowerShell first (Windows 11 compatible)
                String result = executePowerShell(
                        "(Get-CimInstance -ClassName Win32_Processor).ProcessorId");
                if (result.isEmpty()) {
                    // Fallback to WMIC (older Windows)
                    result = executeCommand("wmic cpu get ProcessorId");
                }
                if (result.isEmpty()) {
                    // Last resort: CPU name as identifier
                    result = executePowerShell(
                            "(Get-CimInstance -ClassName Win32_Processor).Name");
                }
                return result.isEmpty() ? "NO_CPU_ID" : result;

            } else if (os.contains("linux")) {
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
     * Windows 11: Uses PowerShell as primary, WMIC as fallback
     */
    private static String getMotherboardSerial() {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                // Try PowerShell first (Windows 11 compatible)
                String result = executePowerShell(
                        "(Get-CimInstance -ClassName Win32_BaseBoard).SerialNumber");
                if (result.isEmpty() || result.equalsIgnoreCase("To be filled by O.E.M.") ||
                        result.equalsIgnoreCase("Default string")) {
                    // Try BIOS serial as alternative
                    result = executePowerShell(
                            "(Get-CimInstance -ClassName Win32_BIOS).SerialNumber");
                }
                if (result.isEmpty()) {
                    // Fallback to WMIC
                    result = executeCommand("wmic baseboard get SerialNumber");
                }
                if (result.isEmpty() || result.equalsIgnoreCase("To be filled by O.E.M.")) {
                    // Use UUID as last resort
                    result = executePowerShell(
                            "(Get-CimInstance -ClassName Win32_ComputerSystemProduct).UUID");
                }
                return result.isEmpty() ? "NO_MOBO_SERIAL" : result;

            } else if (os.contains("linux")) {
                String result = executeCommand("cat /sys/class/dmi/id/board_serial 2>/dev/null");
                if (result.isEmpty()) {
                    result = getLinuxMachineId();
                }
                return result;

            } else if (os.contains("mac")) {
                return executeCommand(
                        "ioreg -rd1 -c IOPlatformExpertDevice | grep IOPlatformSerialNumber | cut -d'\"' -f4");
            }
        } catch (Exception e) {
            // Fallback silently
        }
        return "NO_MOBO_SERIAL";
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
     * Gets STABLE OS information (WITHOUT username which can change).
     */
    private static String getStableOsInfo() {
        return System.getProperty("os.name") +
                System.getProperty("os.arch");
        // REMOVED: user.name - was causing license invalidation
    }

    /**
     * Executes a PowerShell command and returns the output.
     * Used for Windows 11 compatibility where WMIC is deprecated.
     */
    private static String executePowerShell(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-NonInteractive",
                    "-Command",
                    command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    result.append(line);
                }
            }

            process.waitFor();
            reader.close();

            String output = result.toString().trim();
            // Filter out common placeholder values
            if (output.equalsIgnoreCase("To be filled by O.E.M.") ||
                    output.equalsIgnoreCase("Default string") ||
                    output.equalsIgnoreCase("None") ||
                    output.equalsIgnoreCase("Not Available")) {
                return "";
            }
            return output;

        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Executes a system command and returns the output.
     */
    private static String executeCommand(String command) {
        try {
            Process process;
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                process = Runtime.getRuntime().exec(new String[] { "cmd", "/c", command });
            } else {
                process = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", command });
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
     * Test method to display generated machine ID and debug info.
     */
    public static void main(String[] args) {
        System.out.println("=== Hardware Fingerprint Generator v4.0 ===");
        System.out.println("=== Stable Components Only + Cached ===\n");

        // Test cached performance
        long start1 = System.currentTimeMillis();
        String id1 = generateMachineId();
        long time1 = System.currentTimeMillis() - start1;

        long start2 = System.currentTimeMillis();
        String id2 = generateMachineId();
        long time2 = System.currentTimeMillis() - start2;

        System.out.println("Machine ID: " + id1);
        System.out.println("\nPerformance:");
        System.out.println("  First call:  " + time1 + "ms " + (time1 > 100 ? "(generated new)" : "(from cache)"));
        System.out.println("  Second call: " + time2 + "ms (from cache)");

        System.out.println("\nComponents (for debugging):");
        System.out.println("  Motherboard/BIOS: " + getMotherboardSerial());
        System.out.println("  CPU ID: " + getCpuId());
        System.out.println("  OS Info: " + getStableOsInfo());
        System.out.println("  Linux Machine ID: " + getLinuxMachineId());

        System.out.println("\n----------------------------------------");
        System.out.println("Notes:");
        System.out.println("- Machine ID is CACHED for fast startup");
        System.out.println("- v4.0: Uses only Motherboard + CPU + OS (no volatile MAC/Disk)");
        System.out.println("- Uses PowerShell (Win11) with WMIC fallback");
        System.out.println("- Call clearCache() to regenerate Machine ID");
    }
}