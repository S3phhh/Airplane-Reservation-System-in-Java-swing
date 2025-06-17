import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseManager {
    // --- IMPORTANT: CONFIGURE YOUR DATABASE CONNECTION DETAILS HERE ---
    private static final String DB_URL = "jdbc:mysql://localhost:3306/airplane_reservations"; // Replace with your DB URL
    private static final String DB_USER = "root"; // Replace with your DB username
    private static final String DB_PASSWORD = ""; // Replace with your DB password
    // --- END OF CONFIGURATION ---

    private static Connection connection;

    // Establishes connection to the database
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            } catch (SQLException e) {
                System.err.println("Database connection error: " + e.getMessage());
                throw e; // Re-throw to be handled by caller
            }
        }
        return connection;
    }

    // Closes the database connection
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null; // Reset connection
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }
    }

    public static void addAuditLogEntry(String message, String username) {
        String sql = "INSERT INTO audit_log (message, username) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, message);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding audit log: " + e.getMessage());
        }
    }

    public static List<String> getAuditLogs() {
        List<String> logs = new ArrayList<>();
        String sql = "SELECT log_timestamp, message, username FROM audit_log ORDER BY log_timestamp DESC LIMIT 200";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(String.format("%s - %s (User: %s)",
                        rs.getTimestamp("log_timestamp").toString(),
                        rs.getString("message"),
                        rs.getString("username") == null ? "System" : rs.getString("username")));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching audit logs: " + e.getMessage());
        }
        return logs;
    }

    public static AirplaneReservationSystem.User validateUser(String username, String password) {
        String sql = "SELECT user_id, username, role, loyalty_points FROM users WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    AirplaneReservationSystem.Role role = AirplaneReservationSystem.Role.valueOf(rs.getString("role"));
                    AirplaneReservationSystem.User user = new AirplaneReservationSystem.User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            password,
                            role
                    );
                    user.setLoyaltyPoints(rs.getInt("loyalty_points"));
                    user.setBookingPNRs(getUserBookingPnrs(user.getUserId()));
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error validating user: " + e.getMessage());
        }
        return null;
    }

    public static boolean registerUser(String username, String password, AirplaneReservationSystem.Role role) {
        String checkSql = "SELECT user_id FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
            checkPstmt.setString(1, username);
            try (ResultSet rs = checkPstmt.executeQuery()) {
                if (rs.next()) {
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking username existence: " + e.getMessage());
            return false;
        }

        String insertSql = "INSERT INTO users (username, password, role, loyalty_points) VALUES (?, ?, ?, 0)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role.name());
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
        }
        return false;
    }

    public static void updateUserLoyaltyPoints(int userId, int points) {
        String sql = "UPDATE users SET loyalty_points = ? WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, points);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating loyalty points: " + e.getMessage());
        }
    }

    public static List<AirplaneReservationSystem.Flight> getFlights(boolean isLocal) {
        List<AirplaneReservationSystem.Flight> flights = new ArrayList<>();
        String flightType = isLocal ? "LOCAL" : "INTERNATIONAL";
        String sql = "SELECT flight_id, route, base_fare, total_seats, current_status FROM flights WHERE flight_type = ? ORDER BY route";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, flightType);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AirplaneReservationSystem.Flight flight = new AirplaneReservationSystem.Flight(
                            rs.getInt("flight_id"),
                            rs.getString("route"),
                            rs.getDouble("base_fare"),
                            rs.getInt("total_seats")
                    );
                    flight.setCurrentStatus(AirplaneReservationSystem.FlightStatus.valueOf(rs.getString("current_status")));
                    flights.add(flight);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching flights: " + e.getMessage());
        }
        return flights;
    }

    public static AirplaneReservationSystem.Flight getFlightByRoute(String route) {
        String sql = "SELECT flight_id, route, base_fare, total_seats, current_status FROM flights WHERE route = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, route);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    AirplaneReservationSystem.Flight flight = new AirplaneReservationSystem.Flight(
                            rs.getInt("flight_id"),
                            rs.getString("route"),
                            rs.getDouble("base_fare"),
                            rs.getInt("total_seats")
                    );
                    flight.setCurrentStatus(AirplaneReservationSystem.FlightStatus.valueOf(rs.getString("current_status")));
                    flight.setReservedSeats(getReservedSeatsForFlight(flight.getId()));
                    return flight;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching flight by route: " + e.getMessage());
        }
        return null;
    }

    public static AirplaneReservationSystem.Flight getFlightById(int flightId) {
        String sql = "SELECT flight_id, route, base_fare, total_seats, current_status FROM flights WHERE flight_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, flightId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    AirplaneReservationSystem.Flight flight = new AirplaneReservationSystem.Flight(
                            rs.getInt("flight_id"),
                            rs.getString("route"),
                            rs.getDouble("base_fare"),
                            rs.getInt("total_seats")
                    );
                    flight.setCurrentStatus(AirplaneReservationSystem.FlightStatus.valueOf(rs.getString("current_status")));
                    flight.setReservedSeats(getReservedSeatsForFlight(flight.getId()));
                    return flight;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching flight by ID: " + e.getMessage());
        }
        return null;
    }

    public static void updateFlightStatus(int flightId, AirplaneReservationSystem.FlightStatus status) {
        String sql = "UPDATE flights SET current_status = ? WHERE flight_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setInt(2, flightId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating flight status for flight ID " + flightId + ": " + e.getMessage());
        }
    }

    public static List<AirplaneReservationSystem.Flight> getAllFlightsForStatusUpdate() {
        List<AirplaneReservationSystem.Flight> flights = new ArrayList<>();
        String sql = "SELECT flight_id, route, base_fare, total_seats, current_status FROM flights";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                AirplaneReservationSystem.Flight flight = new AirplaneReservationSystem.Flight(
                        rs.getInt("flight_id"),
                        rs.getString("route"),
                        rs.getDouble("base_fare"),
                        rs.getInt("total_seats")
                );
                flight.setCurrentStatus(AirplaneReservationSystem.FlightStatus.valueOf(rs.getString("current_status")));
                flights.add(flight);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all flights for status update: " + e.getMessage());
        }
        return flights;
    }

    public static AirplaneReservationSystem.Booking createBooking(AirplaneReservationSystem.User user, AirplaneReservationSystem.Flight flight,
                                                                  AirplaneReservationSystem.Fare fare, double totalPrice,
                                                                  Set<String> selectedSeats, int numPersons, String paymentMethod, String pnr) {
        String bookingSql = "INSERT INTO bookings (pnr, user_id, flight_id, fare_class, fare_multiplier, total_price, num_persons, payment_method) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String seatSql = "INSERT INTO reserved_seats (booking_id, flight_id, seat_id_str) VALUES (?, ?, ?)";
        Connection conn = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            long bookingId = -1;
            try (PreparedStatement bookingPstmt = conn.prepareStatement(bookingSql, Statement.RETURN_GENERATED_KEYS)) {
                bookingPstmt.setString(1, pnr);
                bookingPstmt.setInt(2, user.getUserId());
                bookingPstmt.setInt(3, flight.getId());
                bookingPstmt.setString(4, fare.getClassType());
                bookingPstmt.setDouble(5, fare.getMultiplier());
                bookingPstmt.setDouble(6, totalPrice);
                bookingPstmt.setInt(7, numPersons);
                bookingPstmt.setString(8, paymentMethod);
                bookingPstmt.executeUpdate();

                try (ResultSet generatedKeys = bookingPstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        bookingId = generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Creating booking failed, no ID obtained.");
                    }
                }
            }

            try (PreparedStatement seatPstmt = conn.prepareStatement(seatSql)) {
                for (String seatId : selectedSeats) {
                    seatPstmt.setLong(1, bookingId);
                    seatPstmt.setInt(2, flight.getId());
                    seatPstmt.setString(3, seatId);
                    seatPstmt.addBatch();
                }
                seatPstmt.executeBatch();
            }

            conn.commit();

            int pointsEarned = (int) (totalPrice / 100);
            user.addLoyaltyPoints(pointsEarned);
            updateUserLoyaltyPoints(user.getUserId(), user.getLoyaltyPoints());

            return new AirplaneReservationSystem.Booking(
                    (int) bookingId, pnr, user, flight, fare, totalPrice, selectedSeats,
                    false, new java.util.Date(), numPersons, paymentMethod);

        } catch (SQLException e) {
            System.err.println("Booking creation transaction failed: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Rollback failed: " + ex.getMessage());
                }
            }
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) {
                    // Log
                }
            }
        }
    }

    public static Set<String> getReservedSeatsForFlight(int flightId) {
        Set<String> reserved = new HashSet<>();
        String sql = "SELECT seat_id_str FROM reserved_seats WHERE flight_id = ?"; //
        try (Connection conn = getConnection(); //
             PreparedStatement pstmt = conn.prepareStatement(sql)) { //
            pstmt.setInt(1, flightId); //
            try (ResultSet rs = pstmt.executeQuery()) { //
                while (rs.next()) { //
                    reserved.add(rs.getString("seat_id_str")); //
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching reserved seats for flight " + flightId + ": " + e.getMessage()); //
        }
        return reserved; //
    }

    // Helper class for two-phase fetch
    private static class BookingRawData {
        int bookingId; String pnr; int userId; int flightId; String fareClass; double fareMultiplier;
        double totalPrice; int numPersons; java.sql.Timestamp bookingDate; boolean isCheckedIn; String paymentMethod;
        String flightRoute; double flightBaseFare; int flightTotalSeats; String flightStatusStr;
        String userUsername; String userRoleStr; int userLoyaltyPoints;

        BookingRawData(ResultSet rs) throws SQLException {
            this.bookingId = rs.getInt("booking_id");
            this.pnr = rs.getString("pnr");
            this.userId = rs.getInt("user_id");
            this.flightId = rs.getInt("flight_id");
            this.fareClass = rs.getString("fare_class");
            this.fareMultiplier = rs.getDouble("fare_multiplier");
            this.totalPrice = rs.getDouble("total_price");
            this.numPersons = rs.getInt("num_persons");
            this.bookingDate = rs.getTimestamp("booking_date");
            this.isCheckedIn = rs.getBoolean("is_checked_in");
            this.paymentMethod = rs.getString("payment_method");
            this.flightRoute = rs.getString("flight_route");
            this.flightBaseFare = rs.getDouble("flight_base_fare");
            this.flightTotalSeats = rs.getInt("flight_total_seats");
            this.flightStatusStr = rs.getString("flight_status");
            this.userUsername = rs.getString("user_username");
            this.userRoleStr = rs.getString("user_role");
            this.userLoyaltyPoints = rs.getInt("user_loyalty_points");
        }
    }

    public static List<AirplaneReservationSystem.Booking> getUserBookings(int userId) {
        List<AirplaneReservationSystem.Booking> bookings = new ArrayList<>();
        List<BookingRawData> rawDataList = new ArrayList<>();
        String sql = "SELECT b.booking_id, b.pnr, b.user_id, b.flight_id, b.fare_class, b.fare_multiplier, " +
                "b.total_price, b.num_persons, b.booking_date, b.is_checked_in, b.payment_method, " +
                "f.route AS flight_route, f.base_fare AS flight_base_fare, f.total_seats AS flight_total_seats, f.current_status AS flight_status, " +
                "u.username AS user_username, u.role AS user_role, u.loyalty_points AS user_loyalty_points " +
                "FROM bookings b " +
                "JOIN flights f ON b.flight_id = f.flight_id " +
                "JOIN users u ON b.user_id = u.user_id " +
                "WHERE b.user_id = ? ORDER BY b.booking_date DESC";

        // Phase 1: Fetch all raw data
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rawDataList.add(new BookingRawData(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching raw user bookings data for user ID " + userId + ": " + e.getMessage());
            return bookings; // Return empty list on error in phase 1
        }

        // Phase 2: Process raw data and fetch details
        for (BookingRawData raw : rawDataList) {
            try {
                AirplaneReservationSystem.User user = new AirplaneReservationSystem.User(
                        raw.userId, raw.userUsername, null, AirplaneReservationSystem.Role.valueOf(raw.userRoleStr)
                );
                user.setLoyaltyPoints(raw.userLoyaltyPoints);

                AirplaneReservationSystem.Flight flight = new AirplaneReservationSystem.Flight(
                        raw.flightId, raw.flightRoute, raw.flightBaseFare, raw.flightTotalSeats
                );
                flight.setCurrentStatus(AirplaneReservationSystem.FlightStatus.valueOf(raw.flightStatusStr));
                flight.setReservedSeats(getReservedSeatsForFlight(raw.flightId));

                AirplaneReservationSystem.Fare fare = new AirplaneReservationSystem.Fare(
                        raw.fareClass, raw.fareMultiplier
                );

                Set<String> selectedSeats = getSeatsForBooking(raw.bookingId);

                AirplaneReservationSystem.Booking booking = new AirplaneReservationSystem.Booking(
                        raw.bookingId, raw.pnr, user, flight, fare, raw.totalPrice, selectedSeats,
                        raw.isCheckedIn, new java.util.Date(raw.bookingDate.getTime()), raw.numPersons, raw.paymentMethod
                );
                bookings.add(booking);
            } catch (Exception e) {
                System.err.println("Error processing booking PNR " + raw.pnr + " details: " + e.getMessage());
            }
        }
        return bookings;
    }

    private static Set<String> getSeatsForBooking(int bookingId) {
        Set<String> seats = new HashSet<>();
        String sql = "SELECT seat_id_str FROM reserved_seats WHERE booking_id = ?"; //
        try (Connection conn = getConnection(); //
             PreparedStatement pstmt = conn.prepareStatement(sql)) { //
            pstmt.setInt(1, bookingId); //
            try (ResultSet rs = pstmt.executeQuery()) { //
                while (rs.next()) { //
                    seats.add(rs.getString("seat_id_str")); //
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching seats for booking " + bookingId + ": " + e.getMessage()); //
        }
        return seats; //
    }

    private static Set<String> getUserBookingPnrs(int userId) {
        Set<String> pnrs = new HashSet<>();
        String sql = "SELECT pnr FROM bookings WHERE user_id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    pnrs.add(rs.getString("pnr"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user PNRs: " + e.getMessage());
        }
        return pnrs;
    }

    public static boolean cancelBooking(String pnr, int userId) {
        String selectSql = "SELECT booking_id FROM bookings WHERE pnr = ? AND user_id = ?"; // Removed flight_id as it's not used
        Connection conn = null;
        int bookingId = -1;

        try {
            conn = getConnection();
            try (PreparedStatement selPstmt = conn.prepareStatement(selectSql)) {
                selPstmt.setString(1, pnr);
                selPstmt.setInt(2, userId);
                try (ResultSet rs = selPstmt.executeQuery()) {
                    if (rs.next()) {
                        bookingId = rs.getInt("booking_id");
                    } else {
                        System.err.println("Booking PNR " + pnr + " not found for user ID " + userId + " or does not exist.");
                        return false;
                    }
                }
            }

            conn.setAutoCommit(false);

            String deleteSeatsSql = "DELETE FROM reserved_seats WHERE booking_id = ?";
            try (PreparedStatement delSeatsPstmt = conn.prepareStatement(deleteSeatsSql)) {
                delSeatsPstmt.setInt(1, bookingId);
                delSeatsPstmt.executeUpdate();
            }

            String deleteBookingSql = "DELETE FROM bookings WHERE booking_id = ?";
            try (PreparedStatement delBookingPstmt = conn.prepareStatement(deleteBookingSql)) {
                delBookingPstmt.setInt(1, bookingId);
                int affectedRows = delBookingPstmt.executeUpdate();
                if (affectedRows > 0) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error canceling booking PNR " + pnr + ": " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Rollback failed: " + ex.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) {
                    System.err.println("Failed to set auto-commit true: " + ex.getMessage());
                }
            }
        }
    }

    public static boolean updateCheckInStatus(String pnr, boolean isCheckedIn) {
        String sql = "UPDATE bookings SET is_checked_in = ? WHERE pnr = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, isCheckedIn);
            pstmt.setString(2, pnr);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating check-in status for PNR " + pnr + ": " + e.getMessage());
        }
        return false;
    }

    public static AirplaneReservationSystem.Booking getBookingByPnr(String pnrToFind) {
        BookingRawData rawData = null;
        // Ensure 'b.pnr' is selected if BookingRawData constructor expects it.
        String sql = "SELECT b.booking_id, b.pnr, b.user_id, b.flight_id, b.fare_class, b.fare_multiplier, " +
                "b.total_price, b.num_persons, b.booking_date, b.is_checked_in, b.payment_method, " +
                "f.route AS flight_route, f.base_fare AS flight_base_fare, f.total_seats AS flight_total_seats, f.current_status AS flight_status, " +
                "u.username AS user_username, u.role AS user_role, u.loyalty_points AS user_loyalty_points " +
                "FROM bookings b " +
                "JOIN flights f ON b.flight_id = f.flight_id " +
                "JOIN users u ON b.user_id = u.user_id " +
                "WHERE b.pnr = ?";

        // Phase 1: Fetch raw data
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, pnrToFind);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    rawData = new BookingRawData(rs);
                    // rawData.pnr will be set by constructor since 'b.pnr' is in SELECT
                } else {
                    return null; // PNR not found
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching raw booking data by PNR " + pnrToFind + ": " + e.getMessage());
            return null;
        }

        // Phase 2: Process raw data
        if (rawData != null) {
            try {
                AirplaneReservationSystem.User user = new AirplaneReservationSystem.User(
                        rawData.userId, rawData.userUsername, null, AirplaneReservationSystem.Role.valueOf(rawData.userRoleStr)
                );
                user.setLoyaltyPoints(rawData.userLoyaltyPoints);

                AirplaneReservationSystem.Flight flight = new AirplaneReservationSystem.Flight(
                        rawData.flightId, rawData.flightRoute, rawData.flightBaseFare, rawData.flightTotalSeats
                );
                flight.setCurrentStatus(AirplaneReservationSystem.FlightStatus.valueOf(rawData.flightStatusStr));
                flight.setReservedSeats(getReservedSeatsForFlight(rawData.flightId));

                AirplaneReservationSystem.Fare fare = new AirplaneReservationSystem.Fare(
                        rawData.fareClass, rawData.fareMultiplier
                );

                Set<String> selectedSeats = getSeatsForBooking(rawData.bookingId);

                return new AirplaneReservationSystem.Booking(
                        rawData.bookingId, rawData.pnr, user, flight, fare, rawData.totalPrice, selectedSeats,
                        rawData.isCheckedIn, new java.util.Date(rawData.bookingDate.getTime()), rawData.numPersons, rawData.paymentMethod
                );
            } catch (Exception e) {
                System.err.println("Error processing booking details for PNR " + rawData.pnr + " (in getBookingByPnr): " + e.getMessage());
                return null;
            }
        }
        return null;
    }


    public static int getTotalBookingsCount() {
        String sql = "SELECT COUNT(*) FROM bookings";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting total bookings count: " + e.getMessage());
        }
        return 0;
    }

    public static double getTotalRevenue() {
        String sql = "SELECT SUM(total_price) FROM bookings";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting total revenue: " + e.getMessage());
        }
        return 0.0;
    }

    public static int getTotalCheckedInCount() {
        String sql = "SELECT COUNT(*) FROM bookings WHERE is_checked_in = TRUE";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting checked-in count: " + e.getMessage());
        }
        return 0;
    }

    public static double getOverallOccupancy() {
        String totalBookedSeatsSql = "SELECT SUM(num_persons) FROM bookings";
        String totalCapacitySql = "SELECT SUM(total_seats) FROM flights WHERE current_status != 'CANCELED'";
        long totalBookedSeats = 0;
        long totalCapacity = 0;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            try (ResultSet rsBooked = stmt.executeQuery(totalBookedSeatsSql)) {
                if (rsBooked.next()) {
                    totalBookedSeats = rsBooked.getLong(1);
                }
            }
            try (ResultSet rsCapacity = stmt.executeQuery(totalCapacitySql)) {
                if (rsCapacity.next()) {
                    totalCapacity = rsCapacity.getLong(1);
                }
            }
            if (totalCapacity == 0) return 0.0;
            return (double) totalBookedSeats / totalCapacity * 100.0;

        } catch (SQLException e) {
            System.err.println("Error calculating overall occupancy: " + e.getMessage());
        }
        return 0.0;
    }

    public static boolean checkPnrExists(String pnr) {
        String sql = "SELECT 1 FROM bookings WHERE pnr = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, pnr);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking PNR existence: " + e.getMessage());
        }
        return false;
    }
}