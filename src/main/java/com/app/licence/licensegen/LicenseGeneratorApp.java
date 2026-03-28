package com.app.licence.licensegen;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

public class LicenseGeneratorApp extends Application {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final LicenseDatabase db = new LicenseDatabase();
    // Centralized data list for automatic UI updates
    private final ObservableList<LicenseRecord> masterHistoryList = FXCollections.observableArrayList();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Load initial data from DB
        masterHistoryList.addAll(db.getAllLicenses());

        primaryStage.setTitle("HookeXpert - Générateur de Licences");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab generateTab = new Tab("Générer Licence");
        generateTab.setContent(createGenerateView());

        Tab validateTab = new Tab("Valider / Décoder");
        validateTab.setContent(createValidateView());

        Tab localIdTab = new Tab("Mon ID Machine");
        localIdTab.setContent(createLocalIdView());

        Tab historyTab = new Tab("Historique");
        historyTab.setContent(createHistoryView());

        tabPane.getTabs().addAll(generateTab, validateTab, localIdTab, historyTab);

        Scene scene = new Scene(tabPane, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // ==================== VIEW: GENERATE LICENSE ====================
//    private VBox createGenerateView() {
//        VBox layout = new VBox(15);
//        layout.setPadding(new Insets(20));
//
//        Label headerLabel = new Label("Générer une nouvelle clé");
//        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
//
//        Label clientLabel = new Label("Nom du Client / Référence:");
//        TextField clientField = new TextField();
//
//        Label idLabel = new Label("ID Machine du Client (32 caractères):");
//        TextField machineIdField = new TextField();
//        machineIdField.setStyle("-fx-font-family: 'Consolas';");
//
//        Label durationLabel = new Label("Durée de validité:");
//        ComboBox<String> durationBox = new ComboBox<>();
//        durationBox.getItems().addAll("30 Jours", "90 Jours", "365 Jours", "3650 Jours (10 Ans)", "Personnalisé");
//        durationBox.getSelectionModel().select(2);
//
//        Button generateBtn = new Button("Générer la Clé");
//        generateBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
//
//        TextArea resultArea = new TextArea();
//        resultArea.setEditable(false);
//        resultArea.setPrefHeight(100);
//
//        Label statusLabel = new Label("");
//
//        generateBtn.setOnAction(e -> {
//            String clientName = clientField.getText().trim();
//            String mid = machineIdField.getText().trim().toUpperCase();
//
//            if (clientName.isEmpty() || mid.isEmpty()) {
//                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez remplir tous les champs.");
//                return;
//            }
//
//            int days = 365; // Default logic simplified for brevity
//            LocalDate expDate = LocalDate.now().plusDays(days);
//
//            try {
//                String key = LicenseManager.generateLicenseKey(mid, expDate);
//
//                // 1. SAVE TO DATABASE
//                LicenseRecord newRecord = db.addLicense(clientName, mid, key, expDate);
//
//                // 2. INSTANT REFRESH: Add to the observable list
//                if (newRecord != null) {
//                    masterHistoryList.add(0, newRecord);
//                }
//
//                resultArea.setText(key);
//                statusLabel.setText("✅ Succès! Enregistré dans l'historique.");
//                statusLabel.setTextFill(Color.GREEN);
//            } catch (Exception ex) {
//                showAlert(Alert.AlertType.ERROR, "Erreur", "Echec: " + ex.getMessage());
//            }
//        });
//
//        layout.getChildren().addAll(headerLabel, new Separator(), clientLabel, clientField, idLabel, machineIdField, durationLabel, durationBox, generateBtn, statusLabel, resultArea);
//        return layout;
//    }


    // In LicenseGeneratorApp.java inside createGenerateView()

    private VBox createGenerateView() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label headerLabel = new Label("Générer une nouvelle clé");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        Label clientLabel = new Label("Nom du Client / Référence:");
        TextField clientField = new TextField();

        Label idLabel = new Label("ID Machine du Client (32 caractères):");
        TextField machineIdField = new TextField();
        machineIdField.setStyle("-fx-font-family: 'Consolas';");

        // 1. ADD ROLE SELECTOR
        Label roleLabel = new Label("Type de Licence (Privilèges):");
        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("Utilisateur Standard", "Administrateur");
        roleBox.getSelectionModel().select(0); // Default to User

        // 2. Duration Selector (Existing code)
        Label durationLabel = new Label("Durée de validité:");
        ComboBox<String> durationBox = new ComboBox<>();
        durationBox.getItems().addAll("15 Jours", "30 Jours", "90 Jours", "365 Jours", "3650 Jours (10 Ans)", "Personnalisé");
        durationBox.getSelectionModel().select(3); // Select 365 days by default

        // 2. ADD Custom Days Input Field (Hidden by default)
        HBox customDaysBox = new HBox(10);
        Label customDaysLabel = new Label("Nombre de jours:");
        TextField customDaysField = new TextField();
        customDaysField.setPromptText("Ex: 7");
        customDaysBox.getChildren().addAll(customDaysLabel, customDaysField);
        customDaysBox.setVisible(false);
        customDaysBox.setManaged(false); // Don't take up space when hidden

        // Logic to show/hide custom field
        durationBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isCustom = "Personnalisé".equals(newVal);
            customDaysBox.setVisible(isCustom);
            customDaysBox.setManaged(isCustom);
        });

        Button generateBtn = new Button("Générer la Clé");
        generateBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");

        TextArea resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setPrefHeight(100);

        Label statusLabel = new Label("");

        generateBtn.setOnAction(e -> {
            String clientName = clientField.getText().trim();
            String mid = machineIdField.getText().trim().toUpperCase();

            // Map ComboBox selection to code
            String selectedRole = roleBox.getSelectionModel().getSelectedIndex() == 1 ? "ADMIN" : "USER";

            if (clientName.isEmpty() || mid.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez remplir tous les champs.");
                return;
            }

            // 3. LOGIC to determine Days and Status
            int days = 0;
            String selection = durationBox.getValue();

            try {
                if (selection.startsWith("15")) days = 15;
                else if (selection.startsWith("30")) days = 30;
                else if (selection.startsWith("90")) days = 90;
                else if (selection.startsWith("3650")) days = 3650;
                else if (selection.startsWith("365")) days = 365;
                else if (selection.equals("Personnalisé")) {
                    // Parse custom field
                    days = Integer.parseInt(customDaysField.getText().trim());
                    if (days <= 0) throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez entrer un nombre de jours valide.");
                return;
            }

            // Determine Status based on your rule: <= 30 is ESSAI (Trial)
            String licenseStatus = (days <= 30) ? "ESSAI" : "ACTIVE";

            LocalDate expDate = LocalDate.now().plusDays(days);

            try {
//                String key = LicenseManager.generateLicenseKey(mid, expDate);

                // 3. GENERATE KEY WITH ROLE
                String key = LicenseManager.generateLicenseKey(mid, expDate, selectedRole);

//                // Update DB
//                db.addLicense(clientName, mid, key, expDate, licenseStatus);

                // 4. Update DB Call to include status
                LicenseRecord newRecord = db.addLicense(clientName, mid, key, expDate, licenseStatus, selectedRole);

                if (newRecord != null) {
                    masterHistoryList.addFirst(newRecord);
                }

                resultArea.setText(key);
                statusLabel.setText("✅ Succès! Licence (" + licenseStatus + ") générée pour " + days + " jours.");
                statusLabel.setTextFill(Color.GREEN);
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Echec: " + ex.getMessage());
            }
        });

        // Add customDaysBox to the layout children
        layout.getChildren().addAll(headerLabel, new Separator(), clientLabel, clientField, idLabel, machineIdField, roleLabel, roleBox, durationLabel, durationBox, customDaysBox, generateBtn, statusLabel, resultArea);
//        layout.getChildren().addAll(headerLabel, new Separator(), clientLabel, clientField, idLabel, machineIdField, roleLabel, roleBox, durationLabel, durationBox, generateBtn, statusLabel, resultArea);
        return layout;
    }

    // ==================== VIEW: HISTORY (WITH INSTANT REFRESH) ====================
//    private VBox createHistoryView() {
//        VBox layout = new VBox(15);
//        layout.setPadding(new Insets(20));
//
//        Label header = new Label("Historique des Licences");
//        header.setFont(Font.font("System", FontWeight.BOLD, 18));
//
//        HBox searchBox = new HBox(10);
//        TextField searchField = new TextField();
//        searchField.setPromptText("Rechercher client ou ID...");
//        searchField.setPrefWidth(300);
//
//        Button viewDetailsBtn = new Button("👁 Voir Détails Complets");
//        viewDetailsBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
//        searchBox.getChildren().addAll(searchField, viewDetailsBtn);
//
//        // Bind TableView to the ObservableList for instant updates
//        TableView<LicenseRecord> table = new TableView<>(masterHistoryList);
//
//        TableColumn<LicenseRecord, String> colClient = new TableColumn<>("Client");
//        colClient.setCellValueFactory(new PropertyValueFactory<>("clientName"));
//        colClient.setPrefWidth(150);
//
//        TableColumn<LicenseRecord, String> colDate = new TableColumn<>("Généré le");
//        colDate.setCellValueFactory(new PropertyValueFactory<>("generationDate"));
//
//        TableColumn<LicenseRecord, String> colExp = new TableColumn<>("Expire le");
//        colExp.setCellValueFactory(new PropertyValueFactory<>("expirationDate"));
//
//        TableColumn<LicenseRecord, String> colMid = new TableColumn<>("Machine ID");
//        colMid.setCellValueFactory(new PropertyValueFactory<>("machineId"));
//        colMid.setPrefWidth(200);
//
//        table.getColumns().addAll(colClient, colDate, colExp, colMid);
//
//        // Detail View Action
//        viewDetailsBtn.setOnAction(e -> {
//            LicenseRecord selected = table.getSelectionModel().getSelectedItem();
//            if (selected != null) showDetailsDialog(selected);
//        });
//
//        table.setRowFactory(tv -> {
//            TableRow<LicenseRecord> row = new TableRow<>();
//            row.setOnMouseClicked(event -> {
//                if (event.getClickCount() == 2 && (!row.isEmpty())) {
//                    showDetailsDialog(row.getItem());
//                }
//            });
//            return row;
//        });
//
//        // Live Search Filtering
//        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
//            if (newVal.isEmpty()) {
//                table.setItems(masterHistoryList);
//            } else {
//                table.setItems(FXCollections.observableArrayList(db.search(newVal)));
//            }
//        });
//
//        VBox.setVgrow(table, Priority.ALWAYS);
//        layout.getChildren().addAll(header, searchBox, table);
//        return layout;
//    }

    private VBox createHistoryView() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label header = new Label("Historique des Licences");
        header.setFont(Font.font("System", FontWeight.BOLD, 18));

        // Search Bar and Detail Button
        HBox searchBox = new HBox(10);
        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher client ou ID...");
        searchField.setPrefWidth(300);

        Button viewDetailsBtn = new Button("👁 Voir Détails Complets");
        viewDetailsBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        searchBox.getChildren().addAll(searchField, viewDetailsBtn);

        // Bind TableView to the ObservableList for instant updates
        TableView<LicenseRecord> table = new TableView<>(masterHistoryList);

        // Existing Columns
        TableColumn<LicenseRecord, String> colClient = new TableColumn<>("Client");
        colClient.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        colClient.setPrefWidth(120);

        // NEW: Role Column
        TableColumn<LicenseRecord, String> colRole = new TableColumn<>("Type");
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setPrefWidth(80);
        colRole.setStyle("-fx-alignment: CENTER;");
        colRole.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("ADMIN".equals(item)) {
                        setStyle("-fx-text-fill: white; -fx-background-color: #E74C3C; -fx-alignment: CENTER; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: black; -fx-background-color: #AED6F1; -fx-alignment: CENTER;");
                    }
                }
            }
        });

        TableColumn<LicenseRecord, String> colDate = new TableColumn<>("Généré le");
        colDate.setCellValueFactory(new PropertyValueFactory<>("generationDate"));

        TableColumn<LicenseRecord, String> colExp = new TableColumn<>("Expire le");
        colExp.setCellValueFactory(new PropertyValueFactory<>("expirationDate"));

        TableColumn<LicenseRecord, String> colMid = new TableColumn<>("Machine ID");
        colMid.setCellValueFactory(new PropertyValueFactory<>("machineId"));
        colMid.setPrefWidth(180);

        // NEW: Full License Key Column (Read-Only)
        TableColumn<LicenseRecord, String> colFullKey = new TableColumn<>("Clé de Licence");
        colFullKey.setCellValueFactory(new PropertyValueFactory<>("licenseKey"));
        colFullKey.setPrefWidth(350);

        // Custom Cell Factory to make the key selectable but read-only
        colFullKey.setCellFactory(column -> new TableCell<LicenseRecord, String>() {
            private final TextField textField = new TextField();
            {
                textField.setEditable(false);
                // Styling to make it blend into the table cell
                textField.setStyle("-fx-background-color: transparent; " +
                        "-fx-background-insets: 0; " +
                        "-fx-padding: 2; " +
                        "-fx-font-family: 'Consolas'; " +
                        "-fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    textField.setText(item);
                    setGraphic(textField);
                }
            }
        });

        // NEW: Action Column for Instant Copying
        TableColumn<LicenseRecord, Void> colAction = new TableColumn<>("Action");
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button copyBtn = new Button("📋 Copier");
            {
                copyBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #e0e0e0;");
                copyBtn.setOnAction(event -> {
                    LicenseRecord record = getTableView().getItems().get(getIndex());
                    if (record != null) {
                        copyToClipboard(record.getLicenseKey()); // Task 1: Auto-copy to clipboard
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : copyBtn);
            }
        });

        // colFullKey remains for visibility, but the button handles the "automatic" copy
        table.getColumns().addAll(colClient, colRole, colDate, colExp, colMid, colFullKey);

//        table.getColumns().addAll(colClient, colDate, colExp, colMid, colFullKey);

        // Logic for Detail View
        viewDetailsBtn.setOnAction(e -> {
            LicenseRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showDetailsDialog(selected);
        });

        table.setRowFactory(tv -> {
            TableRow<LicenseRecord> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    showDetailsDialog(row.getItem());
                }
            });
            return row;
        });

        // Live Search Filtering
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                table.setItems(masterHistoryList);
            } else {
                table.setItems(FXCollections.observableArrayList(db.search(newVal)));
            }
        });

        VBox.setVgrow(table, Priority.ALWAYS);
        layout.getChildren().addAll(header, searchBox, table);
        return layout;
    }

    private void showDetailsDialog(LicenseRecord record) {
        Stage dialog = new Stage();
        dialog.setTitle("Détails - " + record.getClientName());

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        TextArea keyArea = new TextArea(record.getLicenseKey());
        keyArea.setWrapText(true);
        keyArea.setEditable(false);
        keyArea.setPrefHeight(200);
        keyArea.setStyle("-fx-font-family: 'Consolas';");

        Button copyBtn = new Button("Copier la Clé");
        copyBtn.setMaxWidth(Double.MAX_VALUE);
        copyBtn.setOnAction(e -> copyToClipboard(record.getLicenseKey()));

        root.getChildren().addAll(new Label("Clé de licence complète pour " + record.getClientName() + ":"), keyArea, copyBtn);

        Scene scene = new Scene(root, 600, 400);
        dialog.setScene(scene);
        dialog.show();
    }

    // ==================== OTHER VIEWS & UTILS ====================
//    private VBox createValidateView() {
//        VBox layout = new VBox(15);
//        layout.setPadding(new Insets(20));
//        layout.getChildren().add(new Label("Interface de Validation"));
//        // Re-add your validation logic here...
//        return layout;
//    }

    // ==================== VIEW: VALIDATE / DECODE LICENSE ====================
    private VBox createValidateView() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label headerLabel = new Label("Valider et Décoder une clé");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        // License Input
        Label keyLabel = new Label("Collez la clé de licence ici:");
        TextArea keyInput = new TextArea();
        keyInput.setWrapText(true);
        keyInput.setPrefHeight(100);
        keyInput.setPromptText("Base64 string...");
        keyInput.setStyle("-fx-font-family: 'Consolas';");

        // Optional Machine ID check
        Label midLabel = new Label("Comparer avec ID Machine (Optionnel):");
        TextField midInput = new TextField();
        midInput.setPromptText("Laisser vide pour ignorer la vérification matérielle");

        Button validateBtn = new Button("Vérifier et Décoder la Clé");
        validateBtn.setStyle("-fx-background-color: #673AB7; -fx-text-fill: white; -fx-font-weight: bold;");
        validateBtn.setPrefWidth(250);

        // Results Display Area
        VBox resultsBox = new VBox(12);
        resultsBox.setPadding(new Insets(15));
        resultsBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label resMachine = new Label("Machine ID: -");
        Label resClient = new Label("Client: -"); // New Label for Task 2
        Label resRole = new Label("Type: -"); // NEW LABEL
        Label resDate = new Label("Expiration: -");
        Label resStatus = new Label("Statut: En attente de saisie");
        resStatus.setFont(Font.font("System", FontWeight.BOLD, 13));

        resultsBox.getChildren().addAll(
                new Label("Détails du Décodage:"),
                new Separator(),
                resMachine,
                resClient,
                resRole,
                resDate,
                resStatus
        );

        // LOGIC
        validateBtn.setOnAction(e -> {
            String key = keyInput.getText().trim();
            String checkMid = midInput.getText().trim().toUpperCase();

            if (key.isEmpty()) {
                resStatus.setText("Statut: ❌ Veuillez coller une clé");
                resStatus.setTextFill(Color.RED);
                return;
            }

            try {
                // 1. Decode Base64
                String decoded = new String(Base64.getDecoder().decode(key));
                String[] parts = decoded.split("\\|");

//                if (parts.length != 3) {
//                    resStatus.setText("Statut: ❌ Format de clé corrompu (Manque des segments)");
//                    resStatus.setTextFill(Color.RED);
//                    return;
//                }

//                String licMid = parts[0];
//                LocalDate expDate = LocalDate.parse(parts[1]);
                String licMid, roleStr;
                LocalDate expirationDate;
                // String signature = parts[2]; // Used by LicenseManager for internal validation

                // Handle both Old (3 parts) and New (4 parts) formats
                if (parts.length == 4) {
                    licMid = parts[0];
                    expirationDate = LocalDate.parse(parts[1]);
                    roleStr = parts[2];
                    // parts[3] is signature
                } else if (parts.length == 3) {
                    licMid = parts[0];
                    expirationDate = LocalDate.parse(parts[1]);
                    roleStr = "USER (Legacy)";
                } else {
                    throw new IllegalArgumentException("Invalid Format");
                }

                // 2. Fetch Client Name from masterHistoryList (Synced with DB)
                String foundClient = masterHistoryList.stream()
                        .filter(r -> r.getLicenseKey().equals(key))
                        .map(LicenseRecord::getClientName)
                        .findFirst()
                        .orElse("Inconnu (Clé externe)");


                // 3. Update UI with data
                resMachine.setText("Machine ID: " + licMid);
                resClient.setText("Client: " + foundClient); // Show Client Name
                resDate.setText("Expiration: " + expirationDate.format(DATE_FORMAT));
                resRole.setText("Type: " + roleStr); // Show Role

                // 4. Validation Logic
                boolean expired = LocalDate.now().isAfter(expirationDate);
                boolean midMatch = true;

                if (!checkMid.isEmpty() && !licMid.equalsIgnoreCase(checkMid)) {
                    midMatch = false;
                }

                // 5. Final Status Construction
                StringBuilder statusMsg = new StringBuilder();
                if (expired) {
                    statusMsg.append("❌ EXPIRÉE ");
                    resStatus.setTextFill(Color.RED);
                } else {
                    statusMsg.append("✅ VALIDE ");
                    resStatus.setTextFill(Color.GREEN);
                }

                if (!checkMid.isEmpty()) {
                    if (midMatch) {
                        statusMsg.append("| ✅ MACHINE CORRESPOND");
                    } else {
                        statusMsg.append("| ❌ ID MACHINE DIFFERENT");
                        resStatus.setTextFill(Color.RED);
                    }
                }

                resStatus.setText("Statut: " + statusMsg.toString());

            } catch (Exception ex) {
                resStatus.setText("Statut: ❌ Erreur de lecture (Format invalide)");
                resStatus.setTextFill(Color.RED);
                resMachine.setText("Machine ID: -");
                resDate.setText("Expiration: -");
            }
        });

        layout.getChildren().addAll(
                headerLabel,
                new Separator(),
                keyLabel,
                keyInput,
                midLabel,
                midInput,
                validateBtn,
                resultsBox
        );

        return layout;
    }

    private VBox createLocalIdView() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        TextField idField = new TextField(HardwareFingerprint.generateMachineId());
        idField.setEditable(false);
        Button copyBtn = new Button("Copier ID");
        copyBtn.setOnAction(e -> copyToClipboard(idField.getText()));
        layout.getChildren().addAll(new Label("ID de cette machine:"), idField, copyBtn);
        return layout;
    }

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