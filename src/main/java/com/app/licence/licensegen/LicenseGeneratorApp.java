package com.app.licence.licensegen;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * JavaFX GUI for License Generation and Validation.
 * Replaces the CLI LicenseGenerator.
 */
public class LicenseGeneratorApp extends Application {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final LicenseDatabase db = new LicenseDatabase();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("HookeXpert - Générateur de Licences");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // --- Tab 1: Generate License ---
        Tab generateTab = new Tab("Générer Licence");
        generateTab.setContent(createGenerateView());

        // --- Tab 2: Validate License ---
        Tab validateTab = new Tab("Valider / Décoder");
        validateTab.setContent(createValidateView());

        // --- Tab 3: Local Machine ID ---
        Tab localIdTab = new Tab("Mon ID Machine");
        localIdTab.setContent(createLocalIdView());

        // --- Tab 4: Local Machine ID ---
        Tab historyTab = new Tab("History");
        historyTab.setContent(createHistoryView());

        tabPane.getTabs().addAll(generateTab, validateTab, localIdTab, historyTab);

        Scene scene = new Scene(tabPane, 800, 550);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // ==================== VIEW: GENERATE LICENSE ====================
    private VBox createGenerateView() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.TOP_LEFT);

        // NEW: Client Name Input
        Label clientLabel = new Label("Nom du Client / Référence:");
        TextField clientField = new TextField();
        clientField.setPromptText("Ex: Entreprise XYZ");

        // Header
        Label headerLabel = new Label("Générer une nouvelle clé");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        // Machine ID Input
        Label idLabel = new Label("ID Machine du Client (32 caractères):");
        TextField machineIdField = new TextField();
        machineIdField.setPromptText("Ex: A1B2C3D4...");
        machineIdField.setStyle("-fx-font-family: 'Consolas', 'Monospaced';");

        // Duration Selection
        Label durationLabel = new Label("Durée de validité:");
        ComboBox<String> durationBox = new ComboBox<>();
        durationBox.getItems().addAll(
                "30 Jours (1 Mois)",
                "90 Jours (3 Mois)",
                "365 Jours (1 An)",
                "3650 Jours (Illimité/10 Ans)",
                "Personnalisé"
        );
        durationBox.getSelectionModel().select(2); // Default to 1 year

        // Custom Days Input (Hidden by default)
        HBox customDaysBox = new HBox(10);
        Label customLabel = new Label("Nombre de jours:");
        TextField customDaysField = new TextField("7");
        customDaysField.setPrefWidth(80);
        customDaysBox.getChildren().addAll(customLabel, customDaysField);
        customDaysBox.setVisible(false);
        customDaysBox.setManaged(false);

        // Show/Hide custom input based on selection
        durationBox.setOnAction(e -> {
            boolean isCustom = "Personnalisé".equals(durationBox.getValue());
            customDaysBox.setVisible(isCustom);
            customDaysBox.setManaged(isCustom);
        });

        // Generate Button
        Button generateBtn = new Button("Générer la Clé");
        generateBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        generateBtn.setPrefWidth(200);

        // Output Area
        Label resultLabel = new Label("Clé de licence générée:");
        TextArea resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setPrefHeight(100);
        resultArea.setStyle("-fx-font-family: 'Consolas', 'Monospaced';");

        // Copy Button
        Button copyBtn = new Button("Copier dans le presse-papier");
        copyBtn.setDisable(true);

        // Status Label
        Label statusLabel = new Label("");

        // LOGIC
        generateBtn.setOnAction(e -> {

            String clientName = clientField.getText().trim(); // Capture name
            String mid = machineIdField.getText().trim().toUpperCase();
            statusLabel.setText("");
            statusLabel.setTextFill(Color.BLACK);

            // Validation
            if (clientName.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Le nom du client est requis pour le suivi.");
                return;
            }

            if (mid.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "L'ID Machine est requis.");
                return;
            }

            if (mid.length() != 32) {
                // Warning but allow
                statusLabel.setText("⚠️ Attention: ID de taille incorrecte (" + mid.length() + "/32)");
                statusLabel.setTextFill(Color.ORANGE);
            }

            int days = 365;
            try {
                String choice = durationBox.getValue();
                if (choice.startsWith("30 ")) days = 30;
                else if (choice.startsWith("90 ")) days = 90;
                else if (choice.startsWith("365 ")) days = 365;
                else if (choice.startsWith("3650")) days = 3650;
                else {
                    days = Integer.parseInt(customDaysField.getText().trim());
                }
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Nombre de jours invalide.");
                return;
            }

            // Generate
            LocalDate expDate = LocalDate.now().plusDays(days);
            try {
                String key = LicenseManager.generateLicenseKey(mid, expDate);

                // SAVE TO DATABASE
                db.addLicense(clientName, mid, key, expDate);

                resultArea.setText(key);
                copyBtn.setDisable(false);
                if (statusLabel.getText().isEmpty()) {
                    statusLabel.setText("✅ Succès! Expire le " + expDate.format(DATE_FORMAT));
                    statusLabel.setTextFill(Color.GREEN);
                }
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Echec de génération: " + ex.getMessage());
            }
        });

        copyBtn.setOnAction(e -> copyToClipboard(resultArea.getText()));

        layout.getChildren().addAll(
                headerLabel,
                new Separator(),
                clientLabel, clientField, // <--- Add these
                idLabel, machineIdField,
                durationLabel, durationBox, customDaysBox,
                new Separator(),
                generateBtn,
                statusLabel,
                resultLabel, resultArea, copyBtn
        );

        return layout;
    }

    private VBox createHistoryView() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label header = new Label("Historique des Licences");
        header.setFont(Font.font("System", FontWeight.BOLD, 18));

        // Search Bar
        HBox searchBox = new HBox(10);
        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher client ou ID...");
        searchField.setPrefWidth(300);
        Button refreshBtn = new Button("Actualiser / Rechercher");
        searchBox.getChildren().addAll(searchField, refreshBtn);

        // Table
        TableView<LicenseRecord> table = new TableView<>();

        TableColumn<LicenseRecord, String> colClient = new TableColumn<>("Client");
        colClient.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        colClient.setPrefWidth(150);

        TableColumn<LicenseRecord, String> colDate = new TableColumn<>("Généré le");
        colDate.setCellValueFactory(new PropertyValueFactory<>("generationDate"));

        TableColumn<LicenseRecord, String> colExp = new TableColumn<>("Expire le");
        colExp.setCellValueFactory(new PropertyValueFactory<>("expirationDate"));

        TableColumn<LicenseRecord, String> colMid = new TableColumn<>("Machine ID");
        colMid.setCellValueFactory(new PropertyValueFactory<>("machineId"));
        colMid.setPrefWidth(220);

        TableColumn<LicenseRecord, String> colKey = new TableColumn<>("Clé (Début)");
        colKey.setCellValueFactory(cell -> {
            String k = cell.getValue().getLicenseKey();
            return new javafx.beans.property.SimpleStringProperty(k.substring(0, 10) + "...");
        });
        colKey.setPrefWidth(100);

        table.getColumns().addAll(colClient, colDate, colExp, colMid, colKey);

        // Context Menu (Right Click to Copy Key)
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copyItem = new MenuItem("Copier la clé complète");
        copyItem.setOnAction(e -> {
            LicenseRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) copyToClipboard(selected.getLicenseKey());
        });
        contextMenu.getItems().add(copyItem);
        table.setContextMenu(contextMenu);

        // Load Data Logic
        Runnable loadData = () -> {
            String q = searchField.getText().trim();
            if (q.isEmpty()) {
                table.getItems().setAll(db.getAllLicenses());
            } else {
                table.getItems().setAll(db.search(q));
            }
        };

        refreshBtn.setOnAction(e -> loadData.run());

        // Initial Load
        loadData.run();

        layout.getChildren().addAll(header, searchBox, table);
        return layout;
    }

    // ==================== VIEW: VALIDATE LICENSE ====================
    private VBox createValidateView() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label headerLabel = new Label("Valider et Décoder une clé");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        // License Input
        Label keyLabel = new Label("Collez la clé de licence ici:");
        TextArea keyInput = new TextArea();
        keyInput.setWrapText(true);
        keyInput.setPrefHeight(80);

        // Optional Machine ID check
        Label midLabel = new Label("Comparer avec ID Machine (Optionnel):");
        TextField midInput = new TextField();
        midInput.setPromptText("Laisser vide pour ignorer la vérification matérielle");

        Button validateBtn = new Button("Vérifier la Clé");
        validateBtn.setPrefWidth(200);

        // Results
        VBox resultsBox = new VBox(10);
        resultsBox.setStyle("-fx-background-color: #f4f4f4; -fx-padding: 10; -fx-background-radius: 5;");
        Label resMachine = new Label("Machine ID: -");
        Label resDate = new Label("Expiration: -");
        Label resStatus = new Label("Statut: -");

        resultsBox.getChildren().addAll(resMachine, resDate, new Separator(), resStatus);

        validateBtn.setOnAction(e -> {
            String key = keyInput.getText().trim();
            String checkMid = midInput.getText().trim();

            if (key.isEmpty()) {
                resStatus.setText("Statut: ❌ Clé vide");
                return;
            }

            try {
                // Decode Logic (Replicated from LicenseGenerator to allow checking arbitrary IDs)
                String decoded = new String(Base64.getDecoder().decode(key));
                String[] parts = decoded.split("\\|");

                if (parts.length != 3) {
                    resStatus.setText("Statut: ❌ Format invalide");
                    resStatus.setTextFill(Color.RED);
                    return;
                }

                String licMid = parts[0];
                LocalDate expDate = LocalDate.parse(parts[1]);

                resMachine.setText("Machine ID: " + licMid);
                resDate.setText("Expiration: " + expDate.format(DATE_FORMAT));

                // Validation Checks
                boolean expired = LocalDate.now().isAfter(expDate);
                boolean midMatch = true;
                if (!checkMid.isEmpty() && !licMid.equalsIgnoreCase(checkMid)) {
                    midMatch = false;
                }

                StringBuilder sb = new StringBuilder();
                if (expired) sb.append("❌ EXPIRÉE ");
                else sb.append("✅ DATES OK ");

                if (!checkMid.isEmpty()) {
                    if (midMatch) sb.append("| ✅ MACHINE OK");
                    else sb.append("| ❌ MACHINE DIFFERENTE");
                }

                resStatus.setText("Statut: " + sb.toString());
                resStatus.setTextFill(expired || !midMatch ? Color.RED : Color.GREEN);
                resStatus.setFont(Font.font("System", FontWeight.BOLD, 12));

            } catch (Exception ex) {
                resStatus.setText("Statut: ❌ Erreur de lecture (" + ex.getMessage() + ")");
                resStatus.setTextFill(Color.RED);
            }
        });

        layout.getChildren().addAll(
                headerLabel, new Separator(),
                keyLabel, keyInput,
                midLabel, midInput,
                validateBtn,
                resultsBox
        );
        return layout;
    }

    // ==================== VIEW: LOCAL MACHINE ID ====================
    private VBox createLocalIdView() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);

        Label title = new Label("Identifiant de CETTE Machine");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label desc = new Label("Utilisez cet ID pour générer des licences de test\npour cet ordinateur.");
        desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        TextField idField = new TextField();
        idField.setEditable(false);
        idField.setAlignment(Pos.CENTER);
        idField.setStyle("-fx-font-size: 14px; -fx-font-family: 'Consolas'; -fx-background-color: #eee;");

        // Load ID
        idField.setText(HardwareFingerprint.generateMachineId());

        Button copyBtn = new Button("Copier l'ID");
        copyBtn.setPrefSize(150, 40);
        copyBtn.setOnAction(e -> copyToClipboard(idField.getText()));

        layout.getChildren().addAll(title, desc, idField, copyBtn);
        return layout;
    }

    // ==================== UTILS ====================
    private void copyToClipboard(String text) {
        if (text != null && !text.isEmpty()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}