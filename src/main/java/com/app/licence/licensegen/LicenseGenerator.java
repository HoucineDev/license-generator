package com.app.licence.licensegen;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * License Key Generator Tool for HookeXpert.
 *
 * RUN THIS ON YOUR MACHINE (the developer's machine) to generate
 * license keys for clients.
 *
 * WORKFLOW:
 * 1. Client runs HookeXpert → sees expiration dialog
 * 2. Client copies their Machine ID and sends it to you (email, phone, etc.)
 * 3. You run this tool, enter their Machine ID and license duration
 * 4. You send the generated license key back to the client
 * 5. Client enters the key → App activates!
 *
 * IMPORTANT: Keep this file ONLY on your development machine.
 * Do NOT include it in the client distribution!
 */
public class LicenseGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        printBanner();

        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("Options:");
            System.out.println("  1. Générer une clé de licence");
            System.out.println("  2. Valider une clé de licence");
            System.out.println("  3. Afficher mon ID Machine");
            System.out.println("  4. Quitter");
            System.out.print("\nChoix [1-4]: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    generateLicense(scanner);
                    break;
                case "2":
                    validateLicense(scanner);
                    break;
                case "3":
                    showMachineId();
                    break;
                case "4":
                    System.out.println("\nAu revoir!");
                    scanner.close();
                    return;
                default:
                    System.out.println("Option invalide. Veuillez réessayer.");
            }
        }
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║          HOOKEXPERT - GÉNÉRATEUR DE LICENCES             ║");
        System.out.println("║                    Version 1.0                           ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("⚠️  OUTIL RÉSERVÉ AU DÉVELOPPEUR - NE PAS DISTRIBUER");
    }

    private static void generateLicense(Scanner scanner) {
        System.out.println("\n--- GÉNÉRATION DE LICENCE ---\n");

        // Get client's machine ID
        System.out.print("ID Machine du client: ");
        String machineId = scanner.nextLine().trim().toUpperCase();

        if (machineId.isEmpty()) {
            System.out.println("❌ Erreur: L'ID Machine ne peut pas être vide.");
            return;
        }

        if (machineId.length() != 32) {
            System.out.println("⚠️  Attention: L'ID Machine devrait faire 32 caractères (actuellement: " + machineId.length() + ")");
            System.out.print("Continuer quand même? [o/N]: ");
            if (!scanner.nextLine().trim().toLowerCase().startsWith("o")) {
                return;
            }
        }

        // Get license duration
        System.out.println("\nDurée de la licence:");
        System.out.println("  1. 30 jours");
        System.out.println("  2. 90 jours (3 mois)");
        System.out.println("  3. 365 jours (1 an)");
        System.out.println("  4. Illimité (10 ans)");
        System.out.println("  5. Personnalisé");
        System.out.print("Choix [1-5]: ");

        String durationChoice = scanner.nextLine().trim();
        int days;

        switch (durationChoice) {
            case "1":
                days = 30;
                break;
            case "2":
                days = 90;
                break;
            case "3":
                days = 365;
                break;
            case "4":
                days = 3650; // ~10 years
                break;
            case "5":
                System.out.print("Nombre de jours: ");
                try {
                    days = Integer.parseInt(scanner.nextLine().trim());
                    if (days <= 0) {
                        System.out.println("❌ Erreur: Le nombre de jours doit être positif.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("❌ Erreur: Nombre invalide.");
                    return;
                }
                break;
            default:
                System.out.println("❌ Option invalide.");
                return;
        }

        // Calculate expiration date
        LocalDate expirationDate = LocalDate.now().plusDays(days);

        // Generate license key
        String licenseKey = LicenseManager.generateLicenseKey(machineId, expirationDate);

        // Display result
        System.out.println("\n" + "═".repeat(60));
        System.out.println("✅ CLÉ DE LICENCE GÉNÉRÉE AVEC SUCCÈS");
        System.out.println("═".repeat(60));
        System.out.println();
        System.out.println("📋 Informations:");
        System.out.println("   Machine ID:  " + machineId);
        System.out.println("   Valide pour: " + days + " jours");
        System.out.println("   Expire le:   " + expirationDate.format(DATE_FORMAT));
        System.out.println();
        System.out.println("🔑 Clé de licence (à envoyer au client):");
        System.out.println("─".repeat(60));
        System.out.println(licenseKey);
        System.out.println("─".repeat(60));
        System.out.println();
        System.out.println("📧 Copiez cette clé et envoyez-la à votre client.");
    }

    private static void validateLicense(Scanner scanner) {
        System.out.println("\n--- VALIDATION DE LICENCE ---\n");

        System.out.print("Clé de licence à valider: ");
        String licenseKey = scanner.nextLine().trim();

        if (licenseKey.isEmpty()) {
            System.out.println("❌ Erreur: La clé ne peut pas être vide.");
            return;
        }

        System.out.print("ID Machine (laisser vide pour utiliser cette machine): ");
        String machineId = scanner.nextLine().trim();

        if (machineId.isEmpty()) {
            machineId = HardwareFingerprint.generateMachineId();
            System.out.println("   Utilisation de l'ID Machine local: " + machineId);
        }

        // Decode and validate
        try {
            String decoded = new String(java.util.Base64.getDecoder().decode(licenseKey));
            String[] parts = decoded.split("\\|");

            if (parts.length != 3) {
                System.out.println("\n❌ Format de clé invalide.");
                return;
            }

            String licensedMachineId = parts[0];
            LocalDate expirationDate = LocalDate.parse(parts[1]);

            System.out.println("\n" + "─".repeat(60));
            System.out.println("📋 Détails de la licence:");
            System.out.println("   Machine ID:   " + licensedMachineId);
            System.out.println("   Expiration:   " + expirationDate.format(DATE_FORMAT));

            // Check machine match
            boolean machineMatch = licensedMachineId.equals(machineId.toUpperCase());
            System.out.println("   Machine OK:   " + (machineMatch ? "✅ Oui" : "❌ Non (ne correspond pas)"));

            // Check expiration
            boolean notExpired = !LocalDate.now().isAfter(expirationDate);
            System.out.println("   Non expirée:  " + (notExpired ? "✅ Oui" : "❌ Non (expirée)"));

            // Overall status
            if (machineMatch && notExpired) {
                System.out.println("\n✅ LICENCE VALIDE");
            } else {
                System.out.println("\n❌ LICENCE INVALIDE");
            }
            System.out.println("─".repeat(60));

        } catch (Exception e) {
            System.out.println("\n❌ Erreur lors de la validation: " + e.getMessage());
        }
    }

    private static void showMachineId() {
        System.out.println("\n--- ID MACHINE LOCAL ---\n");

        String machineId = HardwareFingerprint.generateMachineId();

        System.out.println("L'ID Machine de cet ordinateur est:");
        System.out.println();
        System.out.println("  " + machineId);
        System.out.println();
        System.out.println("(Utile pour tester les licences sur votre propre machine)");
    }
}
