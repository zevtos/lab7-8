package ru.itmo.client.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import ru.itmo.client.MainApp;
import ru.itmo.client.utility.runtime.Runner;
import ru.itmo.general.models.Ticket;

import java.util.ResourceBundle;

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
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
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

    private ObservableList<Ticket> ticketData = FXCollections.observableArrayList();

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setRunner(Runner runner) {
        this.runner = runner;
    }

    @FXML
    private void initialize() {
        addButton.setOnAction(event -> handleAdd());
        updateButton.setOnAction(event -> handleUpdate());
        deleteButton.setOnAction(event -> handleDelete());

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

        // Set the observable list data to the table
        dataTable.setItems(ticketData);

        // Listen for selection changes and show the ticket details when changed
        dataTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showTicketDetails(newValue));
    }

    @FXML
    private void handleAdd() {
        Ticket newTicket = new Ticket();
        boolean okClicked = mainApp.showTicketEditDialog(newTicket);
        if (okClicked) {
            runner.addTicket(newTicket);  // Assuming you have a runner that handles the business logic
            ticketData.add(newTicket);
        }
    }

    @FXML
    private void handleUpdate() {
        Ticket selectedTicket = dataTable.getSelectionModel().getSelectedItem();
        if (selectedTicket != null) {
            boolean okClicked = mainApp.showTicketEditDialog(selectedTicket);
            if (okClicked) {
                runner.updateTicket(selectedTicket);  // Assuming you have a runner that handles the business logic
                showTicketDetails(selectedTicket);
            }
        } else {
            showAlert(
                    bundle.getString("update.error.title"),
                    bundle.getString("update.error.header"),
                    bundle.getString("update.error.content")
            );
        }
    }

    @FXML
    private void handleDelete() {
        int selectedIndex = dataTable.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            Ticket selectedTicket = dataTable.getItems().get(selectedIndex);
            runner.deleteTicket(selectedTicket);  // Assuming you have a runner that handles the business logic
            dataTable.getItems().remove(selectedIndex);
        } else {
            showAlert(
                    bundle.getString("delete.error.title"),
                    bundle.getString("delete.error.header"),
                    bundle.getString("delete.error.content")
            );
        }
    }

    private void showTicketDetails(Ticket ticket) {
        if (ticket != null) {
            // Fill the labels with info from the ticket object
            nameLabel.setText(ticket.getName());
            coordinatesLabel.setText(ticket.getCoordinates().toString());
            creationDateLabel.setText(ticket.getCreationDate().toString());
            priceLabel.setText(Double.toString(ticket.getPrice()));
            discountLabel.setText(ticket.getDiscount().toString());
            commentLabel.setText(ticket.getComment());
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
        } else {
            // Ticket is null, remove all the text
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
        }
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(mainApp.getPrimaryStage());
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        alert.showAndWait();
    }
}