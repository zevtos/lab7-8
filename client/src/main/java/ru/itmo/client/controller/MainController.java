package ru.itmo.client.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import org.controlsfx.control.Notifications;
import ru.itmo.client.MainApp;
import ru.itmo.client.utility.runtime.Runner;
import ru.itmo.general.models.Ticket;
import ru.itmo.general.network.Response;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MainController {

    private MainApp mainApp;
    private Runner runner;
    private ResourceBundle bundle;

    @FXML
    private TableView<Ticket> dataTable;
    @FXML
    private TableColumn<Ticket, Integer> idColumn;
    @FXML
    private TableColumn<Ticket, String> nameColumn;
    @FXML
    private TableColumn<Ticket, String> coordinatesColumn;
    @FXML
    private TableColumn<Ticket, String> creationDateColumn;
    @FXML
    private TableColumn<Ticket, Double> priceColumn;
    @FXML
    private TableColumn<Ticket, Long> discountColumn;
    @FXML
    private TableColumn<Ticket, String> commentColumn;
    @FXML
    private TableColumn<Ticket, String> typeColumn;
    @FXML
    private TableColumn<Ticket, String> columnBirthday;
    @FXML
    private TableColumn<Ticket, String> columnHeight;
    @FXML
    private TableColumn<Ticket, String> columnPassportID;
    @FXML
    private TableColumn<Ticket, String> columnHairColor;
    @FXML
    private TableColumn<Ticket, Integer> userIdColumn;
    @FXML
    private CheckBox filterCheckBox;
    @FXML
    private ComboBox<String> themeSelector;
    @Setter
    private Stage primaryStage;
    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button clearButton;
    @FXML
    private Button helpButton;
    @FXML
    public Button addIfMinButton;
    @FXML
    public Button sumOfPriceButton;
    @FXML
    public Button executeScriptButton;
    // Labels for ticket details
    @FXML
    private Label nameLabel;
    @FXML
    private Label coordinatesLabel;
    @FXML
    private Label creationDateLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label discountLabel;
    @FXML
    private Label commentLabel;
    @FXML
    private Label typeLabel;
    @FXML
    private Label birthdayLabel;
    @FXML
    private Label heightLabel;
    @FXML
    private Label passportIDLabel;
    @FXML
    private Label hairColorLabel;
    @FXML
    private Label userInfoLabel;
    @Getter
    private ObservableList<Ticket> ticketData = FXCollections.observableArrayList();
    private boolean backgroundTaskRunning = false;
    private Thread backgroundThread;
    private Thread scriptThread;
    private Thread fetchThread;
    private boolean fetchThreadRunning = false;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setRunner(Runner runner) {
        this.runner = runner;
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    @FXML
    private void initialize() {
        addButton.setOnAction(event -> handleAdd());
        updateButton.setOnAction(event -> handleUpdate());
        deleteButton.setOnAction(event -> handleDelete());
        clearButton.setOnAction(event -> handleClear());

        filterCheckBox.setOnAction(event -> {
            if (filterCheckBox.isSelected()) {
                fetchUserTickets();
            } else {
                fetchTickets();
            }
        });

        // Initialize the table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        coordinatesColumn.setCellValueFactory(new PropertyValueFactory<>("coordinates"));
        creationDateColumn.setCellValueFactory(new PropertyValueFactory<>("creationDate"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        discountColumn.setCellValueFactory(new PropertyValueFactory<>("discount"));
        commentColumn.setCellValueFactory(new PropertyValueFactory<>("comment"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        columnBirthday.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPerson().birthday().toString()));
        columnHeight.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPerson().height().toString()));
        columnPassportID.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPerson().passportID()));
        columnHairColor.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPerson().hairColor().toString()));
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        // Set the observable list data to the table
        dataTable.setItems(ticketData);
        // Listen for selection changes and show the ticket details when changed
        dataTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showTicketDetails(newValue));

        startBackgroundUpdateThread();
    }

    private void startBackgroundUpdateThread() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                while (true) {
                    TimeUnit.SECONDS.sleep(10);
                    Platform.runLater(() -> handleFilter());
                }
            }
        };

        backgroundThread = new Thread(task);
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }

    private void handleFilter() {
        if (filterCheckBox.isSelected()) {
            ObservableList<Ticket> filteredData = FXCollections.observableArrayList();
            for (Ticket ticket : runner.fetchTickets()) {
                if (ticket.getUserId().equals(runner.getCurrentUserId())) {
                    filteredData.add(ticket);
                }
            }
            Platform.runLater(() -> {
                ticketData.setAll(filteredData);
                dataTable.setItems(ticketData);
                dataTable.refresh();
                dataTable.sort();
            });
        } else {
            fetchTickets();
        }
    }

    @FXML
    private void handleAdd() {
        Ticket newTicket = new Ticket();

        boolean okClicked = mainApp.showTicketEditDialog(newTicket);

        if (okClicked) {
            newTicket.setUserId(runner.getCurrentUserId());

            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    return runner.addTicket(newTicket);
                }

                @Override
                protected void succeeded() {
                    Boolean added = getValue();
                    if (added) {
                        Platform.runLater(() -> {
                            dataTable.getItems().add(newTicket);
                            dataTable.refresh();
                            dataTable.sort();

                            DataVisualizationController dataVisualizationController = mainApp.getDataVisualizationController();
                            if (dataVisualizationController != null) {
                                dataVisualizationController.addTicket(newTicket);
                            }

                            Notifications.create()
                                    .title("Ticket Added")
                                    .text("The ticket was successfully added." + '\n' + "Assigned id: " + newTicket.getId())
                                    .hideAfter(Duration.seconds(3))
                                    .position(Pos.BOTTOM_RIGHT)
                                    .showInformation();
                        });
                    }
                }

                @Override
                protected void failed() {
                    Platform.runLater(() -> showAlert("Error", "Failed to add ticket", getException().getMessage()));
                }
            };

            startBackgroundTask(task);
        }
    }

    @FXML
    private void handleUpdate() {
        Ticket selectedTicket = dataTable.getSelectionModel().getSelectedItem();
        if (selectedTicket != null) {
            boolean okClicked = mainApp.showTicketEditDialog(selectedTicket);
            if (okClicked) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() {
                        runner.updateTicket(selectedTicket);
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        DataVisualizationController dataVisualizationController = mainApp.getDataVisualizationController();
                        if (dataVisualizationController != null) {
                            dataVisualizationController.updateTicket(selectedTicket);
                        }
                        Platform.runLater(() -> {
                            showTicketDetails(selectedTicket);
                            dataTable.refresh();
                        });
                    }

                    @Override
                    protected void failed() {
                        Platform.runLater(() -> showAlert("Error", "Failed to update ticket", getException().getMessage()));
                    }
                };

                startBackgroundTask(task);
            }
        } else {
            showAlert(bundle.getString("update.error.title"),
                    bundle.getString("update.error.header"),
                    bundle.getString("update.error.content"));
        }
    }

    @FXML
    private void handleDelete() {
        int selectedIndex = dataTable.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            Ticket selectedTicket = dataTable.getItems().get(selectedIndex);
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    runner.deleteTicket(selectedTicket);
                    return null;
                }

                @Override
                protected void succeeded() {
                    DataVisualizationController dataVisualizationController = mainApp.getDataVisualizationController();
                    if (dataVisualizationController != null) {
                        dataVisualizationController.removeTicket(selectedTicket);
                    }
                    Platform.runLater(() -> {
                        dataTable.getItems().remove(selectedIndex);
                        dataTable.refresh();
                    });
                }

                @Override
                protected void failed() {
                    Platform.runLater(() -> showAlert("Error", "Failed to delete ticket", getException().getMessage()));
                }
            };

            startBackgroundTask(task);
        } else {
            showAlert(bundle.getString("delete.error.title"),
                    bundle.getString("delete.error.header"),
                    bundle.getString("delete.error.content"));
        }
    }

    @FXML
    private void handleClear() {
        boolean confirmed = MainApp.showConfirmationDialog(
                bundle.getString("clear.confirm.title"),
                bundle.getString("clear.confirm.header"),
                bundle.getString("clear.confirm.content")
        );
        if (confirmed) {
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    return runner.clearTickets();
                }

                @Override
                protected void succeeded() {
                    DataVisualizationController dataVisualizationController = mainApp.getDataVisualizationController();
                    if (dataVisualizationController != null) {
                        dataVisualizationController.clearAllTickets();
                    }
                    Boolean success = getValue();
                    Platform.runLater(() -> {
                        if (success) {
                            ticketData.clear();
                            MainApp.showAlert(
                                    bundle.getString("clear.success.title"),
                                    bundle.getString("clear.success.header"),
                                    bundle.getString("clear.success.content")
                            );
                        } else {
                            MainApp.showAlert(
                                    bundle.getString("clear.error.title"),
                                    bundle.getString("clear.error.header"),
                                    bundle.getString("clear.error.content")
                            );
                        }
                    });
                }

                @Override
                protected void failed() {
                    Platform.runLater(() -> showAlert("Error", "Failed to clear tickets", getException().getMessage()));
                }
            };

            startBackgroundTask(task);
        }
    }

    @FXML
    private void handleHelp() {
        // Display help dialog or message
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.getDefault());
        String helpMessage = bundle.getString("help.general");

        Stage helpStage = new Stage();
        helpStage.initModality(Modality.APPLICATION_MODAL);
        helpStage.setTitle("Help");

        TextArea helpTextArea = new TextArea();
        helpTextArea.setEditable(false);
        helpTextArea.setWrapText(true);
        helpTextArea.setText(helpMessage);
        Button closeButton = new Button("Close");
        closeButton.setOnAction(event -> helpStage.close());

        VBox vbox = new VBox(helpTextArea, closeButton);
        vbox.setSpacing(10);
        vbox.setPadding(new Insets(10));

        Scene scene = new Scene(vbox, 400, 300);
        helpStage.setScene(scene);
        helpStage.show();
    }

    @FXML
    public void handleAddIfMin() {
        Ticket newTicket = new Ticket();

        boolean okClicked = mainApp.showTicketEditDialog(newTicket);

        if (okClicked) {
            newTicket.setUserId(runner.getCurrentUserId()); // Устанавливаем идентификатор пользователя
            boolean added = runner.addTicketIfMin(newTicket); // Assuming you have a runner that handles the business logic

            if (added) {
                ticketData.add(newTicket);

                dataTable.getItems().add(newTicket); // Add the ticket directly to the table
                dataTable.refresh(); // Ensure the table view is refreshed
                dataTable.sort();

                DataVisualizationController dataVisualizationController = mainApp.getDataVisualizationController();
                if (dataVisualizationController != null) {
                    dataVisualizationController.addTicket(newTicket);
                }
                // Create and show notification
                Notifications.create()
                        .title("Ticket Added")
                        .text("The ticket was successfully added." + '\n'
                                + "Assigned id: " + newTicket.getId())
                        .hideAfter(Duration.seconds(3))
                        .position(Pos.BOTTOM_RIGHT)
                        .showInformation();
            } else {
                showAlert("failure.title", bundle.getString("add.failure.header"), bundle.getString("add.failure.content"));
            }
        }
    }

    @FXML
    public void handleSumOfPrice() {
        Response response = runner.sumOfPrice();
        if (response != null) {
            if (response.isSuccess()) {
                showAlert(Alert.AlertType.INFORMATION, "Sum of Prices", response.toString());
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", response.getMessage());
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "No response from server.");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    @FXML
    public void handleExecuteScript() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Script File");
        File file = fileChooser.showOpenDialog(mainApp.getPrimaryStage());
        if (file != null) {
            Task<Runner.ExitCode> task = new Task<>() {
                @Override
                protected Runner.ExitCode call() {
                    return runner.scriptMode(file);
                }

                @Override
                protected void succeeded() {
                    Runner.ExitCode exitCode = getValue();
                    Platform.runLater(() -> {
                        if (exitCode == Runner.ExitCode.OK) {
                            fetchTickets();
                            showAlert(Alert.AlertType.INFORMATION, "Script Execution", "Script executed successfully.");
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Error", "Script execution failed.");
                        }
                    });
                }

                @Override
                protected void failed() {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error", "Script execution failed: " + getException().getMessage()));
                }
            };

            executeScriptInBackground(task);
        }
    }

    private void showTicketDetails(Ticket ticket) {
        if (ticket != null) {
            // Fill the labels with info from the ticket object
            Platform.runLater(() -> {
                nameLabel.setText(ticket.getName());
                coordinatesLabel.setText(ticket.getCoordinates().toString());
                creationDateLabel.setText(ticket.getCreationDate().toString());
                priceLabel.setText(Double.toString(ticket.getPrice()));
                discountLabel.setText(ticket.getDiscount() != null ? ticket.getDiscount().toString() : ""); // Обработка null
                commentLabel.setText(ticket.getComment() != null ? ticket.getComment() : "");
                typeLabel.setText(ticket.getType().toString());

                if (ticket.getPerson() != null) {
                    birthdayLabel.setText(ticket.getPerson().birthday().toString());
                    heightLabel.setText(ticket.getPerson().height().toString());
                    passportIDLabel.setText(ticket.getPerson().passportID());
                    hairColorLabel.setText(ticket.getPerson().hairColor().toString());
                } else {
                    birthdayLabel.setText("");
                    heightLabel.setText("");
                    passportIDLabel.setText("");
                    hairColorLabel.setText("");
                }
            });
        } else {
            // Ticket is null, remove all the text
            Platform.runLater(() -> {
                nameLabel.setText("");
                coordinatesLabel.setText("");
                creationDateLabel.setText("");
                priceLabel.setText("");
                discountLabel.setText("");
                commentLabel.setText("");
                typeLabel.setText("");
                birthdayLabel.setText("");
                heightLabel.setText("");
                passportIDLabel.setText("");
                hairColorLabel.setText("");
            });
        }
    }

    private void showAlert(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(mainApp.getPrimaryStage());
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public void fetchTickets() {
        Task<ObservableList<Ticket>> task = new Task<>() {
            @Override
            protected ObservableList<Ticket> call() {
                Integer userId = runner.getCurrentUserId();
                String username = runner.getCurrentUsername();
                Platform.runLater(() -> userInfoLabel.setText(String.format("%s %s, %s %d",
                        bundle.getString("main.user.info"), username,
                        bundle.getString("main.user.info.id"), userId)));
                return FXCollections.observableArrayList(runner.fetchTickets());
            }

            @Override
            protected void succeeded() {
                ObservableList<Ticket> tickets = getValue();
                if (!tickets.equals(ticketData)) {
                    setRouteData(tickets);
                    Platform.runLater(() -> {
                        ticketData.setAll(tickets);
                        dataTable.setItems(ticketData);
                        dataTable.refresh();
                        dataTable.sort();
                    });
                }
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> showAlert("Error", "Failed to fetch tickets", getException().getMessage()));
            }
        };

        fetchThread = new Thread(task);
        fetchThread.setDaemon(true);
        fetchThread.start();
        fetchThreadRunning = true;

        task.setOnSucceeded(event -> fetchThreadRunning = false);
        task.setOnFailed(event -> fetchThreadRunning = false);
        task.setOnCancelled(event -> fetchThreadRunning = false);
    }

    public void fetchUserTickets() {
        Task<ObservableList<Ticket>> task = new Task<>() {
            @Override
            protected ObservableList<Ticket> call() {
                return FXCollections.observableArrayList(runner.fetchTickets()
                        .stream().filter(ticket -> Objects.equals(ticket.getUserId(), runner.getCurrentUserId())).toList());
            }

            @Override
            protected void succeeded() {
                ObservableList<Ticket> userTickets = getValue();
                if (!userTickets.equals(ticketData)) {
                    setRouteData(userTickets);
                    Platform.runLater(() -> {
                        ticketData.setAll(userTickets);
                        dataTable.setItems(userTickets);
                        dataTable.refresh();
                    });
                }
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> showAlert("Error", "Failed to fetch user tickets", getException().getMessage()));
            }
        };
        fetchThread = new Thread(task);
        fetchThread.setDaemon(true);
        fetchThread.start();
        fetchThreadRunning = true;

        task.setOnSucceeded(event -> fetchThreadRunning = false);
        task.setOnFailed(event -> fetchThreadRunning = false);
        task.setOnCancelled(event -> fetchThreadRunning = false);
    }


    private void startBackgroundTask(Task<?> task) {
        backgroundThread = new Thread(task);
        backgroundThread.setDaemon(true);
        backgroundThread.start();
        backgroundTaskRunning = true;

        task.setOnSucceeded(event -> backgroundTaskRunning = false);
        task.setOnFailed(event -> backgroundTaskRunning = false);
        task.setOnCancelled(event -> backgroundTaskRunning = false);
    }

    private void startScriptTask(Task<?> task) {
        scriptThread = new Thread(task);
        scriptThread.setDaemon(true);
        scriptThread.start();
    }

    private void executeScriptInBackground(Task<?> task) {
        if (scriptThread != null && scriptThread.isAlive()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Script Task Running");
            alert.setHeaderText("A script task is already running.");
            alert.setContentText("Do you want to interrupt the current script task and start a new one?");

            ButtonType buttonTypeYes = new ButtonType("Yes");
            ButtonType buttonTypeNo = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == buttonTypeYes) {
                scriptThread.interrupt();
                startScriptTask(task);
            }
        } else {
            startScriptTask(task);
        }
    }

    public void selectTicket(Ticket ticket) {
        Platform.runLater(() -> dataTable.getSelectionModel().select(ticket));
    }

    private void updateVisualization() {
        DataVisualizationController dataVisualizationController = mainApp.getDataVisualizationController();
        if (dataVisualizationController != null) {
            dataVisualizationController.initializeRoutes(ticketData);
        }
    }

    public void setRouteData(List<Ticket> routes) {
        ticketData.setAll(routes);
        dataTable.setItems(ticketData);

        DataVisualizationController dataVisualizationController = mainApp.getDataVisualizationController();
        if (dataVisualizationController != null) {
            dataVisualizationController.setMainController(this);
            dataVisualizationController.initializeRoutes(routes);
        }
    }
}
