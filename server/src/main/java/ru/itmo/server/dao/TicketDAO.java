package ru.itmo.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.itmo.general.models.*;
import ru.itmo.general.utility.base.Accessible;

import static ru.itmo.server.managers.ConnectionManager.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TicketDAO implements Accessible {
    private static final Logger LOGGER = LoggerFactory.getLogger("TicketDAO");
    private static final String SELECT_ALL_TICKETS_SQL = "SELECT * FROM tickets";
    private static final String CREATE_TICKETS_TABLE_SQL = "CREATE TABLE IF NOT EXISTS tickets (" +
            "id SERIAL PRIMARY KEY," +
            "name VARCHAR NOT NULL," +
            "coordinates_x DOUBLE PRECISION NOT NULL," +
            "coordinates_y FLOAT NOT NULL," +
            "creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "price DOUBLE PRECISION NOT NULL," +
            "discount BIGINT CHECK (discount IS NULL OR (discount > 0 AND discount <= 100))," +
            "comment VARCHAR," +
            "type VARCHAR(20)," +
            "person_birthday TIMESTAMP," +
            "person_height FLOAT," +
            "person_passport_id VARCHAR NOT NULL," +
            "person_hair_color VARCHAR(20) NOT NULL," +
            "user_id INT," +
            "FOREIGN KEY (user_id) REFERENCES users(id))";
    private static final String INSERT_TICKET_SQL = "INSERT INTO tickets (" +
            " name," +
            " coordinates_x," +
            " coordinates_y," +
            " creation_date," +
            " price," +
            " discount," +
            " comment," +
            " type," +
            " person_birthday," +
            " person_height," +
            " person_passport_id," +
            " person_hair_color," +
            " user_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String REMOVE_TICKET_SQL = "DELETE FROM tickets WHERE id = ?";
    private static final String CHECK_TICKET_OWNERSHIP_SQL = "SELECT user_id FROM tickets WHERE id = ?";
    private static final String UPDATE_TICKET_SQL = "UPDATE tickets SET " +
            "name = ?, " +
            "coordinates_x = ?, " +
            "coordinates_y = ?, " +
            "creation_date = ?, " +
            "price = ?, " +
            "discount = ?, " +
            "comment = ?, " +
            "type = ?, " +
            "person_birthday = ?, " +
            "person_height = ?, " +
            "person_passport_id = ?, " +
            "person_hair_color = ? " +
            "WHERE id = ?";

    public TicketDAO() {
    }

    public int addTicket(Ticket ticket, int userId) {
        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(INSERT_TICKET_SQL, Statement.RETURN_GENERATED_KEYS)) {
            set(userId, statement, ticket);

            int rowsAffected = executePrepareUpdate(statement);
            if (rowsAffected > 0) {
                // Get the generated keys (which include the ID of the newly added ticket)
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    // Return the ID of the newly added ticket
                    return generatedKeys.getInt(1);
                } else {
                    // No generated keys found
                    LOGGER.error("Failed to retrieve generated keys after adding ticket");
                    return -1;
                }
            } else {
                LOGGER.error("No rows were affected while adding ticket");
                return -1;
            }
        } catch (SQLException e) {
            LOGGER.error("Error while adding ticket {}", e.getMessage());
            return -1;
        }
    }


    public void addTickets(Collection<Ticket> tickets, int userId) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_TICKET_SQL)) {
            for (Ticket ticket : tickets) {
                set(userId, statement, ticket);
                statement.addBatch();
            }
            int[] results = statement.executeBatch();
            // Check the results array to determine the success of each insertion
            for (int result : results) {
                if (result <= 0) {
                    return; // At least one insertion failed
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error while adding tickets {}", e.getMessage());
        }
    }

    private void set(int userId, PreparedStatement statement, Ticket ticket) throws SQLException {
        set(statement, ticket);
        statement.setInt(13, userId); // User's ID who added the ticket
    }

    private void set(PreparedStatement statement, Ticket ticket) throws SQLException {
        statement.setString(1, ticket.getName());
        statement.setDouble(2, ticket.getCoordinates().getX());
        statement.setFloat(3, ticket.getCoordinates().getY());
        statement.setTimestamp(4, Timestamp.from(ticket.getCreationDate().toInstant()));
        statement.setDouble(5, ticket.getPrice());
        if (ticket.getDiscount() != null) {
            statement.setLong(6, ticket.getDiscount());
        } else {
            statement.setNull(6, Types.BIGINT);
        }
        statement.setString(7, ticket.getComment());
        statement.setString(8, ticket.getType().toString());
        statement.setTimestamp(9, Timestamp.from(ticket.getPerson().getBirthday().toInstant(ZoneOffset.UTC)));
        statement.setFloat(10, ticket.getPerson().getHeight());
        statement.setString(11, ticket.getPerson().getPassportID());
        statement.setString(12, ticket.getPerson().getHairColor().toString());
    }

    public List<Ticket> getAllTickets() {
        List<Ticket> tickets = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_TICKETS_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Ticket ticket = extractTicketFromResultSet(resultSet);
                tickets.add(ticket);
            }
        } catch (SQLException e) {
            LOGGER.error("Error while retrieving tickets from the database: {}", e.getMessage());
        }
        return tickets;
    }

    public boolean removeTicketById(int ticketId) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(REMOVE_TICKET_SQL)) {
            statement.setInt(1, ticketId);
            return executePrepareUpdate(statement) > 0;
        } catch (SQLException e) {
            LOGGER.error("Error while deleting ticket with ID {}: {}", ticketId, e.getMessage());
            return false;
        }
    }

    public boolean updateTicket(Ticket ticket) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_TICKET_SQL)) {

            set(statement, ticket);
            statement.setInt(13, ticket.getId());
            return executePrepareUpdate(statement) > 0;
        } catch (SQLException e) {
            LOGGER.error("Error while updating ticket {}: {}", ticket.getId(), e.getMessage());
            return false;
        }
    }

    public void createTablesIfNotExist() {
        Connection connection = getConnection();
        executeUpdate(connection, CREATE_TICKETS_TABLE_SQL);
    }

    private Ticket extractTicketFromResultSet(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String name = resultSet.getString("name");
        double coordinatesX = resultSet.getDouble("coordinates_x");
        float coordinatesY = resultSet.getFloat("coordinates_y");
        Timestamp creationDateTimestamp = resultSet.getTimestamp("creation_date");
        ZonedDateTime creationDate = creationDateTimestamp.toInstant().atZone(ZoneOffset.UTC);
        double price = resultSet.getDouble("price");
        Long discount = resultSet.getLong("discount");
        if (resultSet.wasNull()) {
            discount = null;
        }
        String comment = resultSet.getString("comment");
        String typeStr = resultSet.getString("type");
        TicketType type = typeStr != null ? TicketType.valueOf(typeStr) : null;

        Timestamp personBirthdayTimestamp = resultSet.getTimestamp("person_birthday");
        LocalDateTime personBirthday = personBirthdayTimestamp != null ?
                personBirthdayTimestamp.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime() : null;

        float personHeight = resultSet.getFloat("person_height");
        String personPassportID = resultSet.getString("person_passport_id");
        String personHairColorStr = resultSet.getString("person_hair_color");
        Color personHairColor = Color.valueOf(personHairColorStr);

        // Assuming Ticket constructor accepts all these parameters
        return new Ticket(id, name, new Coordinates(coordinatesX, coordinatesY), creationDate, price,
                discount, comment, type, new Person(personBirthday, personHeight, personPassportID, personHairColor));
    }

    public boolean removeTicketById(int ticketId, int userID) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(REMOVE_TICKET_SQL)) {

            // Add a check to ensure that the ticket belongs to the specified user
            if (!checkOwnership(ticketId, userID)) {
                LOGGER.error("Ticket with ID {} does not belong to user {}", ticketId, userID);
                return false;
            }

            statement.setInt(1, ticketId);
            return executePrepareUpdate(statement) > 0;
        } catch (SQLException e) {
            LOGGER.error("Error while deleting ticket with ID {}: {}", ticketId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean checkOwnership(int ticketId, int userId) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(CHECK_TICKET_OWNERSHIP_SQL)) {
            statement.setInt(1, ticketId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int ownerId = resultSet.getInt("user_id");
                return ownerId == userId;
            } else {
                return false;
            }
        } catch (SQLException e) {
            LOGGER.error("Error while checking ownership of ticket with ID {}: {}", ticketId, e.getMessage());
            return false;
        }
    }


}