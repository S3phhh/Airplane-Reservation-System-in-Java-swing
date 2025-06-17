import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.TimerTask;
import java.text.SimpleDateFormat;
import java.sql.SQLException; // Added for SQLException

public class AirplaneReservationSystem {

    private JFrame frame;
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel;

    // Logged in user - now fetched from DB
    private User loggedInUser = null;

    private Set<String> selectedSeats = new HashSet<>();
    private Map<String, JButton> seatButtons = new HashMap<>();

    // Temp booking info
    private Flight tempFlight = null; // This will hold a Flight object fetched from DB
    private Fare tempFare = null;
    private int tempPersons = 1;
    private double tempTotalPrice = 0.0;

    // Components for UI references to update
    private JComboBox<String> flightSearchCombo;
    private JComboBox<String> classComboBox;
    private JLabel priceLabel;
    private JSpinner personSpinner;
    private JLabel flightStatusLabel;
    private JLabel weatherLabel;
    private JTextArea chatbotArea;
    private JTextField chatbotInput;
    private JRadioButton radioLocal;
    private JRadioButton radioInternational;

    private JPanel seatPanel;

    private JLabel loyaltyPointsLabel;
    private DefaultListModel<String> bookingListModel;
    private JList<String> bookingList;

    private boolean isDarkMode = false;

    private java.util.Timer uiTimer;

    private JLabel totalBookingsLabel;
    private JLabel revenueLabel;
    private JLabel occupancyLabel;
    private JLabel checkInCountLabel;
    private DefaultListModel<String> auditLogModel;
    private JList<String> auditLogList;

    private Random rand = new Random();

    // FlightStatus enum remains the same
    public enum FlightStatus { ON_TIME, DELAYED, CANCELED }
    public enum Role { ADMIN, CUSTOMER }


    public static void main(String[] args) {
        // IMPORTANT: Ensure MySQL JDBC driver is in your project's classpath!
        // Example: Add mysql-connector-java-8.x.x.jar to your libraries.
        SwingUtilities.invokeLater(() -> {
            // Test DB Connection on startup
            try {
                DatabaseManager.getConnection(); // Establish initial connection
                System.out.println("Database connection successful.");
                DatabaseManager.addAuditLogEntry("System Initialized: DB Connection OK.", "System");
            } catch (SQLException e) {
                System.err.println("FATAL: Could not connect to the database. Application will exit.");
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Could not connect to the database. Please check XAMPP/MySQL server and DatabaseManager settings.\nError: " + e.getMessage(),
                        "Database Connection Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1); // Exit if DB connection fails
            }
            new AirplaneReservationSystem().initUI();
        });
    }

    private void initUI() {
        frame = new JFrame("✈ Airplane Reservation System");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Handle close with DB connection
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DatabaseManager.addAuditLogEntry("System Shutdown Initiated.", loggedInUser != null ? loggedInUser.getUsername() : "System");
                DatabaseManager.closeConnection();
                System.out.println("Database connection closed. Exiting.");
                frame.dispose();
                System.exit(0);
            }
        });

        frame.setSize(1000, 800);
        frame.setLocationRelativeTo(null);

        mainPanel = new JPanel(cardLayout);
        auditLogModel = new DefaultListModel<>();

        addAuditLog("System UI Initialized.");


        mainPanel.add(createLoginPanel(), "Login");
        mainPanel.add(createRegisterPanel(), "Register");
        mainPanel.add(createCustomerPanel(), "CustomerPanel");
        mainPanel.add(createSeatSelectionPanel(), "SeatSelection");
        mainPanel.add(createPaymentPanel(), "Payment");
        mainPanel.add(createBookingListPanel(), "Bookings");
        mainPanel.add(createAdminPanel(), "AdminPanel");
        mainPanel.add(createChatbotPanel(), "Chatbot");

        frame.add(mainPanel);
        frame.setVisible(true);

        cardLayout.show(mainPanel, "Login");
        startUITimer();
    }

    private void startUITimer() {
        uiTimer = new java.util.Timer();
        uiTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    updateFlightStatusesInDb();
                    updateAdminDashboard();
                });
            }
        }, 0, 15000);
    }


    private void stopUITimer() {
        if (uiTimer != null) {
            uiTimer.cancel();
            uiTimer = null;
        }
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(230, 240, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("✈️ Airplane Reservation System", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(new Color(33, 89, 166));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(30, 10, 40, 10);
        panel.add(title, gbc);

        gbc.insets = new Insets(10, 15, 10, 15);

        JLabel userLbl = new JLabel("Username:");
        userLbl.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(userLbl, gbc);

        JTextField userField = new JTextField(20);
        userField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(userField, gbc);

        JLabel passLbl = new JLabel("Password:");
        passLbl.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(passLbl, gbc);

        JPasswordField passField = new JPasswordField(20);
        passField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(passField, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 15));
        btnPanel.setBackground(panel.getBackground());

        JButton loginBtn = createStyledButton("Login", new Color(33, 89, 166), Color.WHITE);
        JButton toRegisterBtn = createStyledButton("Register", new Color(100, 149, 237), Color.WHITE);

        btnPanel.add(loginBtn);
        btnPanel.add(toRegisterBtn);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 20, 10);
        panel.add(btnPanel, gbc);

        ActionListener loginAction = e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter username and password.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            loggedInUser = DatabaseManager.validateUser(username, password);

            if (loggedInUser != null) {
                addAuditLog("User logged in: " + loggedInUser.getUsername());
                userField.setText("");
                passField.setText("");
                if (loggedInUser.getRole() == Role.ADMIN) {
                    updateAdminDashboard();
                    cardLayout.show(mainPanel, "AdminPanel");
                } else {
                    updateCustomerPanelWelcome();
                    updateLoyaltyPoints();
                    cardLayout.show(mainPanel, "CustomerPanel");
                }
            } else {
                addAuditLog("Failed login attempt for username: " + username);
                JOptionPane.showMessageDialog(frame, "Invalid username or password.", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
            }
        };

        loginBtn.addActionListener(loginAction);
        passField.addActionListener(loginAction);
        userField.addActionListener(loginAction);

        toRegisterBtn.addActionListener(e -> {
            userField.setText("");
            passField.setText("");
            cardLayout.show(mainPanel, "Register");
        });
        return panel;
    }

    private JLabel customerWelcomeLabel;

    private void updateCustomerPanelWelcome() {
        if (customerWelcomeLabel != null && loggedInUser != null) {
            customerWelcomeLabel.setText("Welcome, " + loggedInUser.getUsername() + "!");
        }
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(250, 240, 230));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Create New Account", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(165, 110, 60));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(25, 10, 30, 10);
        panel.add(title, gbc);

        gbc.insets = new Insets(10, 15, 10, 15);

        Font labelFont = new Font("Segoe UI", Font.PLAIN, 16);
        Font fieldFont = new Font("Segoe UI", Font.PLAIN, 16);

        JLabel userLbl = new JLabel("Username:");
        userLbl.setFont(labelFont);
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(userLbl, gbc);

        JTextField userField = new JTextField(20);
        userField.setFont(fieldFont);
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(userField, gbc);

        JLabel passLbl = new JLabel("Password:");
        passLbl.setFont(labelFont);
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(passLbl, gbc);

        JPasswordField passField = new JPasswordField(20);
        passField.setFont(fieldFont);
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(passField, gbc);

        JLabel passConfirmLbl = new JLabel("Confirm Password:");
        passConfirmLbl.setFont(labelFont);
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(passConfirmLbl, gbc);

        JPasswordField passConfirmField = new JPasswordField(20);
        passConfirmField.setFont(fieldFont);
        gbc.gridx = 1; gbc.gridy = 3;
        panel.add(passConfirmField, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 15));
        btnPanel.setBackground(panel.getBackground());

        JButton registerBtn = createStyledButton("Register", new Color(165, 110, 60), Color.WHITE);
        JButton backBtn = createStyledButton("Back to Login", new Color(205, 133, 63), Color.WHITE);

        btnPanel.add(registerBtn);
        btnPanel.add(backBtn);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 20, 10);
        panel.add(btnPanel, gbc);

        registerBtn.addActionListener(e -> {
            String username = userField.getText().trim();
            String pass = new String(passField.getPassword());
            String passConfirm = new String(passConfirmField.getPassword());

            if (username.isEmpty() || pass.isEmpty() || passConfirm.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please fill in all fields.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!username.matches("^[a-zA-Z0-9_]{3,15}$")) {
                JOptionPane.showMessageDialog(frame, "Username must be alphanumeric (3-15 characters).", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!pass.equals(passConfirm)) {
                JOptionPane.showMessageDialog(frame, "Passwords do not match.", "Registration Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (pass.length() < 6) {
                JOptionPane.showMessageDialog(frame, "Password must be at least 6 characters.", "Registration Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean success = DatabaseManager.registerUser(username, pass, Role.CUSTOMER);

            if (success) {
                addAuditLog("New user registered: " + username);
                JOptionPane.showMessageDialog(frame, "Registered successfully! Please login.", "Registration Complete", JOptionPane.INFORMATION_MESSAGE);
                userField.setText("");
                passField.setText("");
                passConfirmField.setText("");
                cardLayout.show(mainPanel, "Login");
            } else {
                JOptionPane.showMessageDialog(frame, "Username already exists or database error occurred.", "Registration Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        backBtn.addActionListener(e -> {
            userField.setText("");
            passField.setText("");
            passConfirmField.setText("");
            cardLayout.show(mainPanel, "Login");
        });
        return panel;
    }


    private JPanel createCustomerPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(224, 239, 255));
        topPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel title = new JLabel("Flight Booking System");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(10, 70, 140));
        topPanel.add(title, BorderLayout.WEST);

        JPanel userActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        userActionsPanel.setOpaque(false);

        customerWelcomeLabel = new JLabel("Welcome!");
        customerWelcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        customerWelcomeLabel.setForeground(new Color(50, 50, 50));

        JButton bookingsBtn = createStyledButton("My Bookings", new Color(0, 100, 70), Color.WHITE);
        JButton chatbotBtn = createStyledButton("Ask AI Assistant", new Color(33, 89, 166), Color.WHITE);
        JButton darkModeToggleBtn = createStyledButton("Toggle Dark Mode", new Color(80, 80, 80), Color.WHITE);
        JButton logoutBtn = createStyledButton("Logout", new Color(200, 30, 30), Color.WHITE);

        userActionsPanel.add(customerWelcomeLabel);
        userActionsPanel.add(bookingsBtn);
        userActionsPanel.add(chatbotBtn);
        userActionsPanel.add(darkModeToggleBtn);
        userActionsPanel.add(logoutBtn);
        topPanel.add(userActionsPanel, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Book Your Flight",
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 20), new Color(10, 70, 140)));
        centerPanel.setBackground(new Color(245, 250, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel lblSelectType = new JLabel("Select Flight Type:");
        lblSelectType.setFont(new Font("Segoe UI", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;
        centerPanel.add(lblSelectType, gbc);

        radioLocal = new JRadioButton("Local Flights");
        radioLocal.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        radioLocal.setSelected(true);
        radioLocal.setOpaque(false);

        radioInternational = new JRadioButton("International Flights");
        radioInternational.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        radioInternational.setOpaque(false);

        ButtonGroup group = new ButtonGroup();
        group.add(radioLocal);
        group.add(radioInternational);

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        radioPanel.setOpaque(false);
        radioPanel.add(radioLocal);
        radioPanel.add(radioInternational);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        centerPanel.add(radioPanel, gbc);

        JLabel flightLbl = new JLabel("Available Flights:");
        flightLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;
        centerPanel.add(flightLbl, gbc);

        flightSearchCombo = new JComboBox<>();
        flightSearchCombo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2;
        centerPanel.add(flightSearchCombo, gbc);

        JLabel classLbl = new JLabel("Class:");
        classLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;
        centerPanel.add(classLbl, gbc);

        String[] classes = { "Economy (x1.0)", "Economy Plus (x1.5)", "Business Class (x2.0)", "First Class (x3.0)" };
        classComboBox = new JComboBox<>(classes);
        classComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 2;
        centerPanel.add(classComboBox, gbc);

        JLabel personsLbl = new JLabel("Number of Persons:");
        personsLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;
        centerPanel.add(personsLbl, gbc);

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 9, 1);
        personSpinner = new JSpinner(spinnerModel);
        personSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        JFormattedTextField spinnerText = ((JSpinner.DefaultEditor) personSpinner.getEditor()).getTextField();
        spinnerText.setEditable(false);
        spinnerText.setBackground(Color.WHITE);
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        centerPanel.add(personSpinner, gbc);

        priceLabel = new JLabel("Total Price: ₱0.00");
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        priceLabel.setForeground(new Color(10, 130, 30));
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(15, 12, 15, 12);
        centerPanel.add(priceLabel, gbc);
        gbc.insets = new Insets(8, 12, 8, 12);

        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        actionButtonPanel.setOpaque(false);
        JButton calcPriceBtn = createStyledButton("Calculate Price", new Color(10, 70, 140), Color.WHITE);
        JButton bookBtn = createStyledButton("Proceed to Seat Selection", new Color(10, 125, 70), Color.WHITE);
        actionButtonPanel.add(calcPriceBtn);
        actionButtonPanel.add(bookBtn);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER;
        centerPanel.add(actionButtonPanel, gbc);

        flightStatusLabel = new JLabel("Flight Status: Select a flight");
        flightStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER;
        centerPanel.add(flightStatusLabel, gbc);

        weatherLabel = new JLabel("Weather: -");
        weatherLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER;
        centerPanel.add(weatherLabel, gbc);

        loyaltyPointsLabel = new JLabel("Loyalty Points: 0");
        loyaltyPointsLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        loyaltyPointsLabel.setForeground(new Color(218, 165, 32));
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(15, 12, 10, 12);
        centerPanel.add(loyaltyPointsLabel, gbc);

        panel.add(centerPanel, BorderLayout.CENTER);

        populateFlights(radioLocal.isSelected());

        ItemListener flightTypeListener = e -> populateFlights(radioLocal.isSelected());
        radioLocal.addItemListener(flightTypeListener);
        radioInternational.addItemListener(flightTypeListener);

        ItemListener updateListener = e -> { if (e.getStateChange() == ItemEvent.SELECTED) calculatePrice(); };
        flightSearchCombo.addItemListener(updateListener);
        classComboBox.addItemListener(updateListener);
        personSpinner.addChangeListener(e -> calculatePrice());

        calcPriceBtn.addActionListener(e -> calculatePrice());
        bookBtn.addActionListener(e -> proceedToSeatSelection());

        bookingsBtn.addActionListener(e -> {
            loadUserBookings();
            cardLayout.show(mainPanel, "Bookings");
        });
        chatbotBtn.addActionListener(e -> cardLayout.show(mainPanel, "Chatbot"));
        logoutBtn.addActionListener(e -> logoutUser());
        darkModeToggleBtn.addActionListener(e -> toggleDarkMode());

        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                updateCustomerPanelWelcome();
                updateLoyaltyPoints();
                populateFlights(radioLocal.isSelected());
            }
        });
        return panel;
    }

    private void populateFlights(boolean local) {
        flightSearchCombo.removeAllItems();
        List<Flight> flightsFromDb = DatabaseManager.getFlights(local);

        if (flightsFromDb.isEmpty()) {
            flightSearchCombo.addItem("No flights available for this type.");
            flightSearchCombo.setEnabled(false);
        } else {
            for (Flight flight : flightsFromDb) {
                flightSearchCombo.addItem(flight.getRoute());
            }
            flightSearchCombo.setEnabled(true);
        }

        if (flightSearchCombo.getItemCount() == 0 || !flightSearchCombo.isEnabled()) {
            priceLabel.setText("Total Price: ₱0.00");
            flightStatusLabel.setText("Flight Status: -");
            weatherLabel.setText("Weather: -");
            tempFlight = null;
        } else {
            flightSearchCombo.setSelectedIndex(0);
            calculatePrice();
        }
    }


    private void calculatePrice() {
        String selectedFlightRoute = (String) flightSearchCombo.getSelectedItem();
        String selectedClassStr = (String) classComboBox.getSelectedItem();
        int persons = (int) personSpinner.getValue();

        if (selectedFlightRoute == null || selectedFlightRoute.startsWith("No flights available") || selectedClassStr == null) {
            priceLabel.setText("Total Price: ₱0.00");
            flightStatusLabel.setText("Flight Status: Please select a flight.");
            weatherLabel.setText("Weather: -");
            tempFlight = null;
            return;
        }

        Flight flight = DatabaseManager.getFlightByRoute(selectedFlightRoute);

        if (flight == null) {
            priceLabel.setText("Error: Flight data not found in DB.");
            tempFlight = null;
            return;
        }

        double multiplier = 1.0;
        if (selectedClassStr.contains("Economy Plus (x1.5)")) multiplier = 1.5;
        else if (selectedClassStr.contains("Business Class (x2.0)")) multiplier = 2.0;
        else if (selectedClassStr.contains("First Class (x3.0)")) multiplier = 3.0;

        double totalPrice = flight.getBaseFare() * multiplier * persons;
        priceLabel.setText(String.format("Total Price: ₱%,.2f", totalPrice));

        tempFlight = flight;
        tempFare = new Fare(selectedClassStr.substring(0, selectedClassStr.indexOf(" (x")), multiplier);
        tempPersons = persons;
        tempTotalPrice = totalPrice;

        updateFlightStatusDisplay(flight);
        updateWeatherInfo(flight.getRoute());
    }

    private void updateFlightStatusDisplay(Flight flight) {
        if (flight == null || flightStatusLabel == null) return;

        FlightStatus status = flight.getCurrentStatus();
        String statusText = "Flight Status (" + flight.getRoute() + "): ";
        Color statusColor = Color.BLACK;

        switch (status) {
            case ON_TIME:
                statusText += "On Time";
                statusColor = new Color(0, 128, 0);
                break;
            case DELAYED:
                statusText += "Delayed";
                statusColor = new Color(255, 165, 0);
                break;
            case CANCELED:
                statusText += "Canceled";
                statusColor = Color.RED;
                break;
        }
        flightStatusLabel.setText(statusText);
        flightStatusLabel.setForeground(statusColor);
    }

    private void updateFlightStatusesInDb() {
        List<Flight> allFlights = DatabaseManager.getAllFlightsForStatusUpdate();
        if (allFlights.isEmpty()) return;

        int changes = rand.nextInt(3) + 1;
        for (int i = 0; i < changes; i++) {
            Flight randomFlight = allFlights.get(rand.nextInt(allFlights.size()));
            int r = rand.nextInt(100);
            FlightStatus newStatus = FlightStatus.ON_TIME;
            if (r < 5) newStatus = FlightStatus.CANCELED;
            else if (r < 20) newStatus = FlightStatus.DELAYED;

            if (randomFlight.getCurrentStatus() != newStatus) {
                DatabaseManager.updateFlightStatus(randomFlight.getId(), newStatus);
            }
        }

        if (tempFlight != null && flightStatusLabel != null && flightStatusLabel.isVisible()) {
            Flight updatedTempFlight = DatabaseManager.getFlightById(tempFlight.getId());
            if(updatedTempFlight != null) {
                tempFlight = updatedTempFlight;
                updateFlightStatusDisplay(tempFlight);
            }
        }
    }


    private void updateWeatherInfo(String route) {
        if (route == null || weatherLabel == null) return;
        String[] weathers = {"☀️ Sunny", "☁️ Cloudy", "🌦️ Light Rain", "🌧️ Rainy", "⛈️ Stormy", "🌫️ Foggy", "🌬️ Windy"};
        String depWeather = weathers[rand.nextInt(weathers.length)];
        String destWeather = weathers[rand.nextInt(weathers.length)];
        weatherLabel.setText("Weather for " + route.split(" ")[0] + " → " + route.split(" ")[2] + ": Departure " + depWeather + ", Arrival " + destWeather);
    }

    private void proceedToSeatSelection() {
        if (loggedInUser == null) {
            JOptionPane.showMessageDialog(frame, "Please login first to book a flight.", "Not Logged In", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (tempFlight == null || tempFare == null || tempTotalPrice <= 0) {
            JOptionPane.showMessageDialog(frame, "Please select a flight and calculate the price before proceeding.", "Booking Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Flight currentDbFlight = DatabaseManager.getFlightById(tempFlight.getId());
        if (currentDbFlight == null) {
            JOptionPane.showMessageDialog(frame, "Error fetching flight details. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        tempFlight = currentDbFlight;


        if (tempFlight.getCurrentStatus() == FlightStatus.CANCELED) {
            JOptionPane.showMessageDialog(frame, "This flight (" + tempFlight.getRoute() + ") is currently CANCELED and cannot be booked.", "Flight Canceled", JOptionPane.ERROR_MESSAGE);
            return;
        }

        selectedSeats.clear();
        buildSeatMap(tempFlight);
        if (seatInfoLabel != null) {
            seatInfoLabel.setText("Select exactly " + tempPersons + " seat(s). Green: Available, Orange: Selected, Red: Occupied.");
        }
        cardLayout.show(mainPanel, "SeatSelection");
    }

    private JLabel seatInfoLabel;

    private JPanel createSeatSelectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10,10));
        panel.setBorder(new EmptyBorder(15,15,15,15));
        panel.setBackground(new Color(240, 248, 255));

        JLabel title = new JLabel("Select Your Seat(s)", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(new Color(10, 70, 140));
        panel.add(title, BorderLayout.NORTH);

        seatPanel = new JPanel();
        seatPanel.setLayout(new BoxLayout(seatPanel, BoxLayout.Y_AXIS));
        seatPanel.setBackground(new Color(220, 230, 240));

        JScrollPane scrollPane = new JScrollPane(seatPanel);
        scrollPane.setBorder(BorderFactory.createEtchedBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10,5));
        bottomPanel.setOpaque(false);

        seatInfoLabel = new JLabel("Select seats. Green: Available, Orange: Selected, Red: Occupied.", JLabel.CENTER);
        seatInfoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        seatInfoLabel.setForeground(new Color(80,80,80));
        bottomPanel.add(seatInfoLabel, BorderLayout.NORTH);


        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        btnPanel.setOpaque(false);

        JButton confirmBtn = createStyledButton("Confirm Seat Selection", new Color(10, 130, 30), Color.WHITE);
        JButton cancelBtn = createStyledButton("Cancel & Go Back", new Color(220, 20, 20), Color.WHITE);

        btnPanel.add(confirmBtn);
        btnPanel.add(cancelBtn);
        bottomPanel.add(btnPanel, BorderLayout.CENTER);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        confirmBtn.addActionListener(e -> {
            if (selectedSeats.size() != tempPersons) {
                JOptionPane.showMessageDialog(frame, "You must select exactly " + tempPersons + " seat(s). You have selected " + selectedSeats.size() + ".", "Seat Selection Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Flight currentFlightState = DatabaseManager.getFlightById(tempFlight.getId());
            if (currentFlightState == null) {
                JOptionPane.showMessageDialog(frame, "Error verifying seat availability. Please try again.", "Seat Availability Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!currentFlightState.areSeatsAvailable(selectedSeats)) {
                JOptionPane.showMessageDialog(frame, "Some selected seats are no longer available. Please re-select.", "Seat Availability Error", JOptionPane.ERROR_MESSAGE);
                tempFlight = currentFlightState;
                buildSeatMap(tempFlight);
                return;
            }
            updatePaymentPanelInfo();
            cardLayout.show(mainPanel, "Payment");
        });

        cancelBtn.addActionListener(e -> {
            selectedSeats.clear();
            cardLayout.show(mainPanel, "CustomerPanel");
        });

        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (seatInfoLabel != null) {
                    seatInfoLabel.setText("Select exactly " + tempPersons + " seat(s). Green: Available, Orange: Selected, Red: Occupied.");
                }
            }
        });
        return panel;
    }


    private void buildSeatMap(Flight flight) {
        seatPanel.removeAll();
        seatButtons.clear();

        int totalSeats = flight.getTotalSeats();
        Set<String> flightReservedSeats = flight.getReservedSeats();

        int seatsPerRow = 6;
        int aisleAfter = 3;
        int numRows = (int) Math.ceil((double) totalSeats / seatsPerRow);

        int seatWidth = 55; int seatHeight = 40;
        int hGap = 8; int vGap = 8; int aisleGap = 20;
        Font seatFont = new Font("Arial", Font.BOLD, 12);

        for (int r = 0; r < numRows; r++) {
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, hGap, 0));
            rowPanel.setOpaque(false);
            for (int s = 0; s < seatsPerRow; s++) {
                int currentSeatNumberInFlight = r * seatsPerRow + s + 1;
                if (currentSeatNumberInFlight > totalSeats) break;

                char seatLetter = (char) ('A' + s);
                String seatId = String.valueOf(r + 1) + seatLetter;

                JButton seatBtn = new JButton(seatId);
                seatBtn.setFont(seatFont);
                seatBtn.setPreferredSize(new Dimension(seatWidth, seatHeight));
                seatBtn.setFocusPainted(false);
                seatBtn.setMargin(new Insets(2,2,2,2));

                if (flightReservedSeats.contains(seatId)) {
                    seatBtn.setBackground(new Color(220, 20, 20));
                    seatBtn.setForeground(Color.WHITE);
                    seatBtn.setToolTipText("Occupied");
                    seatBtn.setEnabled(false);
                } else {
                    seatBtn.setBackground(new Color(144, 238, 144));
                    seatBtn.setForeground(Color.BLACK);
                    seatBtn.setToolTipText("Available");
                    seatBtn.setEnabled(true);
                }

                seatBtn.addActionListener(evt -> {
                    JButton clickedButton = (JButton) evt.getSource();
                    String currentSeatId = clickedButton.getText();

                    if (selectedSeats.contains(currentSeatId)) {
                        selectedSeats.remove(currentSeatId);
                        clickedButton.setBackground(new Color(144, 238, 144));
                    } else {
                        if (selectedSeats.size() >= tempPersons) {
                            JOptionPane.showMessageDialog(frame,
                                    "You can select only " + tempPersons + " seat(s). Deselect a seat first.",
                                    "Seat Selection Limit", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        selectedSeats.add(currentSeatId);
                        clickedButton.setBackground(Color.ORANGE);
                    }
                });
                rowPanel.add(seatBtn);
                seatButtons.put(seatId, seatBtn);

                if (s == aisleAfter - 1) {
                    rowPanel.add(Box.createHorizontalStrut(aisleGap));
                }
            }
            seatPanel.add(rowPanel);
            if (r < numRows -1) seatPanel.add(Box.createVerticalStrut(vGap));
        }
        seatPanel.revalidate();
        seatPanel.repaint();
    }

    private JLabel paymentAmountLabel;

    private void updatePaymentPanelInfo() {
        if (paymentAmountLabel != null) {
            paymentAmountLabel.setText(String.format("Amount to Pay: ₱%,.2f", tempTotalPrice));
        }
    }

    private JPanel createPaymentPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(230, 245, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 20, 12, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel title = new JLabel("Complete Your Payment", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(10, 70, 140));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 10, 30, 10);
        panel.add(title, gbc);

        gbc.insets = new Insets(12, 20, 12, 20);

        JLabel paymentLbl = new JLabel("Select Payment Method:");
        paymentLbl.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST;
        panel.add(paymentLbl, gbc);

        JComboBox<String> paymentCombo = new JComboBox<>(new String[] { "Credit/Debit Card", "GCash", "Maya", "PayPal", "Bank Transfer" });
        paymentCombo.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST;
        panel.add(paymentCombo, gbc);

        paymentAmountLabel = new JLabel(String.format("Amount to Pay: ₱%,.2f", tempTotalPrice));
        paymentAmountLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        paymentAmountLabel.setForeground(new Color(10, 130, 30));
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 10, 25, 10);
        panel.add(paymentAmountLabel, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
        btnPanel.setOpaque(false);
        JButton payBtn = createStyledButton("Confirm Payment", new Color(10, 125, 70), Color.WHITE);
        JButton backBtn = createStyledButton("Back to Seat Selection", new Color(200, 30, 30), Color.WHITE);
        btnPanel.add(payBtn);
        btnPanel.add(backBtn);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 10, 10, 10);
        panel.add(btnPanel, gbc);

        payBtn.addActionListener(e -> {
            String paymentMethod = (String) paymentCombo.getSelectedItem();
            JDialog processingDialog = new JDialog(frame, "Processing Payment", true);
            processingDialog.add(new JLabel("Processing payment via " + paymentMethod + "... Please wait.", JLabel.CENTER));
            processingDialog.setSize(350, 100);
            processingDialog.setLocationRelativeTo(frame);

            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                SwingUtilities.invokeLater(() -> {
                    processingDialog.dispose();
                    completeBooking(paymentMethod);
                });
            }).start();
            processingDialog.setVisible(true);
        });

        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "SeatSelection"));

        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                updatePaymentPanelInfo();
            }
        });
        return panel;
    }

    private void completeBooking(String paymentMethod) {
        Flight finalCheckFlight = DatabaseManager.getFlightById(tempFlight.getId());
        if (finalCheckFlight == null || !finalCheckFlight.areSeatsAvailable(selectedSeats)) {
            JOptionPane.showMessageDialog(frame, "Unfortunately, some selected seats became unavailable during payment. Please try selecting seats again.", "Seats Unavailable", JOptionPane.ERROR_MESSAGE);
            if(finalCheckFlight != null) tempFlight = finalCheckFlight;
            buildSeatMap(tempFlight);
            cardLayout.show(mainPanel, "SeatSelection");
            return;
        }

        String pnr = generatePNR();
        Booking newBooking = DatabaseManager.createBooking(loggedInUser, tempFlight, tempFare, tempTotalPrice, // Corrected: loggedInUser
                selectedSeats, tempPersons, paymentMethod, pnr);

        if (newBooking != null) {
            loggedInUser.addBookingPNR(pnr);
            addAuditLog("Booking completed. PNR: " + pnr + ", User: " + loggedInUser.getUsername() +
                    ", Flight: " + tempFlight.getRoute() + ", Amount: " + tempTotalPrice + ", Payment: " + paymentMethod);

            JOptionPane.showMessageDialog(frame,
                    "Payment successful via " + paymentMethod + "!\nYour booking reference (PNR) is: " + pnr + "\n" +
                            "You earned " + (int)(tempTotalPrice/100) + " loyalty points. Total points: " + loggedInUser.getLoyaltyPoints(),
                    "Booking Confirmed!", JOptionPane.INFORMATION_MESSAGE);

            tempFlight = null;
            tempFare = null;
            tempPersons = 1;
            tempTotalPrice = 0.0;
            selectedSeats.clear();

            updateLoyaltyPoints();
            cardLayout.show(mainPanel, "CustomerPanel");
        } else {
            addAuditLog("Booking failed for User: " + loggedInUser.getUsername() + ", Flight: " + tempFlight.getRoute());
            JOptionPane.showMessageDialog(frame, "Booking failed due to a database error. Please try again.", "Booking Failed", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void updateLoyaltyPoints() {
        if (loggedInUser != null && loyaltyPointsLabel != null) {
            loyaltyPointsLabel.setText("Loyalty Points: " + loggedInUser.getLoyaltyPoints() + " ✨");
        }
    }

    private String generatePNR() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder pnr;
        do {
            pnr = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                pnr.append(chars.charAt(rand.nextInt(chars.length())));
            }
        } while (DatabaseManager.checkPnrExists(pnr.toString()));
        return pnr.toString();
    }

    private JPanel createBookingListPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(20, 25, 25, 25));
        panel.setBackground(new Color(230, 245, 240));

        JLabel title = new JLabel("My Bookings", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(new Color(0, 100, 70));
        panel.add(title, BorderLayout.NORTH);

        bookingListModel = new DefaultListModel<>();
        bookingList = new JList<>(bookingListModel);
        bookingList.setFont(new Font("Monospaced", Font.PLAIN, 14));
        bookingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookingList.setCellRenderer(new BookingListRenderer());

        JScrollPane scrollPane = new JScrollPane(bookingList);
        scrollPane.setBorder(BorderFactory.createEtchedBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 12));
        btnPanel.setBackground(panel.getBackground());

        JButton checkInBtn = createStyledButton("Online Check-In", new Color(0, 100, 0), Color.WHITE);
        JButton cancelBookingBtn = createStyledButton("Cancel Booking", new Color(200, 0, 0), Color.WHITE);
        JButton backBtn = createStyledButton("Back to Booking", new Color(0, 60, 130), Color.WHITE);

        btnPanel.add(checkInBtn);
        btnPanel.add(cancelBookingBtn);
        btnPanel.add(backBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        cancelBookingBtn.addActionListener(e -> {
            int idx = bookingList.getSelectedIndex();
            if (idx == -1) {
                JOptionPane.showMessageDialog(frame, "Please select a booking to cancel.", "Selection Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String selectedValue = bookingList.getSelectedValue();
            String pnr = extractPNRFromBookingString(selectedValue);
            if (pnr == null) {
                JOptionPane.showMessageDialog(frame, "Could not identify PNR for selected booking.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Booking bookingToCancel = DatabaseManager.getBookingByPnr(pnr);
            if (bookingToCancel == null) {
                JOptionPane.showMessageDialog(frame, "Booking details not found for PNR: " + pnr, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Are you sure you want to cancel booking PNR: " + pnr + " for flight " + bookingToCancel.getFlight().getRoute() + "?",
                    "Confirm Cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) return;

            boolean success = DatabaseManager.cancelBooking(pnr, loggedInUser.getUserId());
            if (success) {
                loggedInUser.removeBookingPNR(pnr);
                addAuditLog("Booking canceled. PNR: " + pnr + ", User: " + loggedInUser.getUsername());

                JOptionPane.showMessageDialog(frame, "Booking PNR: " + pnr + " canceled successfully.", "Cancellation Confirmed", JOptionPane.INFORMATION_MESSAGE);
                loadUserBookings();
                updateLoyaltyPoints();
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to cancel booking PNR: " + pnr + ". It might not belong to you or a database error occurred.", "Cancellation Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        checkInBtn.addActionListener(e -> {
            int idx = bookingList.getSelectedIndex();
            if (idx == -1) {
                JOptionPane.showMessageDialog(frame, "Please select a booking to check in.", "Selection Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String selectedValue = bookingList.getSelectedValue();
            String pnr = extractPNRFromBookingString(selectedValue);
            if (pnr == null) return;

            Booking booking = DatabaseManager.getBookingByPnr(pnr);
            if (booking == null) {
                JOptionPane.showMessageDialog(frame, "Booking PNR " + pnr + " not found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (booking.isCheckedIn()) {
                JOptionPane.showMessageDialog(frame, "You have already checked in for booking PNR: " + pnr + ".", "Already Checked In", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Flight bookedFlight = DatabaseManager.getFlightById(booking.getFlight().getId());
            if (bookedFlight.getCurrentStatus() == FlightStatus.CANCELED) {
                JOptionPane.showMessageDialog(frame, "Cannot check in. Flight " + bookedFlight.getRoute() + " is CANCELED.", "Check-in Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean success = DatabaseManager.updateCheckInStatus(pnr, true);
            if (success) {
                addAuditLog("Checked in for PNR: " + pnr + ", User: " + loggedInUser.getUsername());
                JOptionPane.showMessageDialog(frame,
                        "Check-in successful for PNR: " + pnr + "!\n" +
                                "Selected seats: " + String.join(", ", booking.getSelectedSeats()),
                        "Check-in Complete", JOptionPane.INFORMATION_MESSAGE);
                loadUserBookings();
            } else {
                JOptionPane.showMessageDialog(frame, "Check-in failed for PNR: " + pnr + ". Database error.", "Check-in Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "CustomerPanel"));

        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadUserBookings();
            }
        });
        return panel;
    }


    class BookingListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String bookingStrFromModel = (String) value;


            String pnr = extractPNRFromBookingString(bookingStrFromModel);
            if (pnr != null) {
                Booking booking = DatabaseManager.getBookingByPnr(pnr);
                if (booking != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
                    String formattedDate = sdf.format(booking.getBookingDate());
                    String checkInIcon = booking.isCheckedIn() ? "✅" : "⏳";
                    Flight flightForBooking = DatabaseManager.getFlightById(booking.getFlight().getId());

                    label.setText(String.format("<html><b>PNR: %s</b> %s<br/>Flight: %s (%s)<br/>Seats: %s | Fare: %s<br/>Total: ₱%,.2f | Booked: %s</html>",
                            pnr, checkInIcon,
                            booking.getFlight().getRoute(),
                            flightForBooking != null ? flightForBooking.getCurrentStatus().toString().replace("_", " ") : "N/A",
                            String.join(", ", booking.getSelectedSeats()),
                            booking.getFare().getClassType(),
                            booking.getTotalPrice(),
                            formattedDate
                    ));
                    label.setBorder(new EmptyBorder(5, 10, 5, 10));
                    if (flightForBooking != null && flightForBooking.getCurrentStatus() == FlightStatus.CANCELED) {
                        label.setForeground(Color.RED);
                    } else if (booking.isCheckedIn()) {
                        label.setForeground(new Color(0,100,0));
                    } else {
                        label.setForeground(Color.BLACK);
                    }
                } else {
                    label.setText("Error loading details for PNR: " + pnr);
                }
            } else {
                label.setText("Invalid booking data in list.");
            }
            return label;
        }
    }


    private String extractPNRFromBookingString(String bookingStr) {
        if (bookingStr == null) return null;
        String pnrPrefix = "PNR: ";
        int pnrStartIndex = bookingStr.indexOf(pnrPrefix);
        if (pnrStartIndex == -1 && bookingStr.contains("<b>PNR: ")) {
            pnrStartIndex = bookingStr.indexOf("<b>PNR: ") + "<b>PNR: ".length();
            int pnrEndIndex = bookingStr.indexOf("</b>", pnrStartIndex);
            if (pnrEndIndex != -1) return bookingStr.substring(pnrStartIndex, pnrEndIndex).trim();
        } else if (pnrStartIndex != -1) {
            pnrStartIndex += pnrPrefix.length();
            int pnrEndIndex = bookingStr.indexOf(" ", pnrStartIndex);
            if (pnrEndIndex == -1) pnrEndIndex = bookingStr.indexOf("|", pnrStartIndex);
            if (pnrEndIndex == -1) pnrEndIndex = bookingStr.length();
            return bookingStr.substring(pnrStartIndex, pnrEndIndex).trim();
        }
        return null;
    }

    private void loadUserBookings() {
        bookingListModel.clear();
        if (loggedInUser == null) {
            bookingListModel.addElement("Error: Not logged in.");
            return;
        }

        List<Booking> userBookingsFromDb = DatabaseManager.getUserBookings(loggedInUser.getUserId());

        if (userBookingsFromDb.isEmpty()) {
            bookingListModel.addElement("No bookings found. Time to plan a trip!");
            bookingList.setEnabled(false);
            return;
        }

        bookingList.setEnabled(true);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");

        for (Booking booking : userBookingsFromDb) {
            String seatsStr = String.join(", ", booking.getSelectedSeats());
            String checkInStatus = booking.isCheckedIn() ? "Checked In ✅" : "Pending Check-In ⏳";
            Flight flightForBooking = DatabaseManager.getFlightById(booking.getFlight().getId());
            String flightStatusStr = (flightForBooking != null) ? flightForBooking.getCurrentStatus().toString().replace("_", " ") : "N/A";
            if (flightForBooking != null && flightForBooking.getCurrentStatus() == FlightStatus.CANCELED) flightStatusStr = "🔴 CANCELED";

            String listEntry = String.format("PNR: %s | Flight: %s | Status: %s | Seats: %s | Price: %,.2f | Date: %s | Check-in: %s",
                    booking.getPnr(),
                    booking.getFlight().getRoute(),
                    flightStatusStr,
                    seatsStr,
                    booking.getTotalPrice(),
                    sdf.format(booking.getBookingDate()),
                    checkInStatus
            );
            bookingListModel.addElement(listEntry);
        }
        if (bookingListModel.isEmpty()) {
            bookingListModel.addElement("No bookings found.");
            bookingList.setEnabled(false);
        }
    }

    private void logoutUser() {
        int confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to logout?", "Logout Confirmation", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            addAuditLog("User logged out: " + (loggedInUser != null ? loggedInUser.getUsername() : "Unknown"));
            loggedInUser = null;
            tempFlight = null;
            tempFare = null;
            tempPersons = 1;
            tempTotalPrice = 0.0;
            selectedSeats.clear();
            if(chatbotArea != null) chatbotArea.setText("Chatbot: Hello! How can I help you today?\n");
            if(chatbotInput != null) chatbotInput.setText("");
            cardLayout.show(mainPanel, "Login");
        }
    }

    private JPanel createAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout(10,10));
        panel.setBorder(new EmptyBorder(10,15,15,15));
        panel.setBackground(new Color(240, 245, 250));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        JLabel title = new JLabel("Administrator Dashboard", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(10, 70, 140));
        topPanel.add(title, BorderLayout.CENTER);

        JButton logoutBtn = createStyledButton("Logout Admin", new Color(200, 30, 30), Color.WHITE);
        logoutBtn.addActionListener(e -> logoutUser());
        JPanel logoutPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logoutPanel.setOpaque(false);
        logoutPanel.add(logoutBtn);
        topPanel.add(logoutPanel, BorderLayout.EAST);
        panel.add(topPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);

        JPanel dashboardPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        dashboardPanel.setOpaque(false);
        dashboardPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "System Statistics",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 18), new Color(10, 70, 140)));

        totalBookingsLabel = createAdminStatLabel("Total Bookings: 0");
        revenueLabel = createAdminStatLabel("Total Revenue: ₱0.00");
        occupancyLabel = createAdminStatLabel("Overall Occupancy: 0.00%");
        checkInCountLabel = createAdminStatLabel("Passengers Checked In: 0");

        dashboardPanel.add(totalBookingsLabel);
        dashboardPanel.add(revenueLabel);
        dashboardPanel.add(occupancyLabel);
        dashboardPanel.add(checkInCountLabel);
        splitPane.setTopComponent(dashboardPanel);

        JPanel auditLogPanel = new JPanel(new BorderLayout(5,5));
        auditLogPanel.setOpaque(false);
        auditLogPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "System Audit Log (Last 200)",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 18), new Color(10, 70, 140)));

        auditLogList = new JList<>(auditLogModel);
        auditLogList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(auditLogList);
        auditLogPanel.add(logScrollPane, BorderLayout.CENTER);
        splitPane.setBottomComponent(auditLogPanel);

        panel.add(splitPane, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.35));

        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                updateAdminDashboard();
                loadAuditLogs();
            }
        });
        return panel;
    }

    private JLabel createAdminStatLabel(String initialText) {
        JLabel label = new JLabel(initialText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 20));
        label.setForeground(new Color(40, 40, 40));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(150,180,200), 1),
                new EmptyBorder(10,15,10,15)
        ));
        return label;
    }


    private void updateAdminDashboard() {
        if (totalBookingsLabel == null) return;

        totalBookingsLabel.setText("Total Bookings: " + DatabaseManager.getTotalBookingsCount());
        revenueLabel.setText(String.format("Total Revenue: ₱%,.2f", DatabaseManager.getTotalRevenue()));
        occupancyLabel.setText(String.format("Overall Occupancy: %.2f%%", DatabaseManager.getOverallOccupancy()));
        checkInCountLabel.setText("Passengers Checked In: " + DatabaseManager.getTotalCheckedInCount());
    }

    private void addAuditLog(String message) {
        DatabaseManager.addAuditLogEntry(message, loggedInUser != null ? loggedInUser.getUsername() : "System");
        if (auditLogModel != null && auditLogList != null && auditLogList.isVisible()) {
            loadAuditLogs();
        }
    }
    private void loadAuditLogs() {
        if (auditLogModel == null) return;
        auditLogModel.clear();
        List<String> logs = DatabaseManager.getAuditLogs();
        for (String log : logs) {
            auditLogModel.addElement(log);
        }
        if (auditLogList != null && auditLogModel.getSize() > 0) {
            auditLogList.ensureIndexIsVisible(0);
        }
    }


    private JPanel createChatbotPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(240, 248, 255));

        JLabel title = new JLabel("AI Flight Assistant", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(new Color(10, 70, 140));
        panel.add(title, BorderLayout.NORTH);

        chatbotArea = new JTextArea();
        chatbotArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        chatbotArea.setEditable(false);
        chatbotArea.setLineWrap(true);
        chatbotArea.setWrapStyleWord(true);
        chatbotArea.setText("🤖 AI Assistant: Hello! How can I assist you with your flight booking today?\n" +
                "   You can ask about: 'flights to [city]', 'booking status [PNR]', 'help'.\n");
        JScrollPane scrollPane = new JScrollPane(chatbotArea);
        scrollPane.setBorder(BorderFactory.createEtchedBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);
        chatbotInput = new JTextField();
        chatbotInput.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        chatbotInput.setToolTipText("Type your query here and press Enter or click Send");
        inputPanel.add(chatbotInput, BorderLayout.CENTER);

        JButton sendBtn = createStyledButton("Send", new Color(33, 89, 166), Color.WHITE);
        inputPanel.add(sendBtn, BorderLayout.EAST);

        JButton backBtn = createStyledButton("Back to Main Menu", new Color(100, 149, 237), Color.WHITE);
        JPanel bottomBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomBtnPanel.setOpaque(false);
        bottomBtnPanel.add(backBtn);

        JPanel southWrapperPanel = new JPanel(new BorderLayout(0,5));
        southWrapperPanel.setOpaque(false);
        southWrapperPanel.add(inputPanel, BorderLayout.NORTH);
        southWrapperPanel.add(bottomBtnPanel, BorderLayout.CENTER);
        panel.add(southWrapperPanel, BorderLayout.SOUTH);

        ActionListener sendAction = e -> processChatbotInput();
        sendBtn.addActionListener(sendAction);
        chatbotInput.addActionListener(sendAction);

        backBtn.addActionListener(e -> {
            if (loggedInUser != null) {
                if (loggedInUser.getRole() == Role.ADMIN) {
                    cardLayout.show(mainPanel, "AdminPanel");
                } else {
                    cardLayout.show(mainPanel, "CustomerPanel");
                }
            } else {
                cardLayout.show(mainPanel, "Login");
            }
        });

        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                chatbotInput.requestFocusInWindow();
            }
        });
        return panel;
    }

    private void processChatbotInput() {
        String userInput = chatbotInput.getText().trim().toLowerCase();
        if (userInput.isEmpty()) return;

        chatbotArea.append("👤 You: " + chatbotInput.getText() + "\n");
        chatbotInput.setText("");

        String botResponse = "🤖 AI Assistant: ";
        if (userInput.startsWith("hello") || userInput.startsWith("hi")) {
            botResponse += "Hello there! How can I help you today?";
        } else if (userInput.contains("flights to")) {
            String destination = userInput.substring(userInput.indexOf("flights to") + "flights to".length()).trim();
            botResponse += "Searching for flights to " + destination + "...\n";
            boolean found = false;
            List<Flight> localDbFlights = DatabaseManager.getFlights(true);
            List<Flight> intDbFlights = DatabaseManager.getFlights(false);

            for (Flight flight : localDbFlights) {
                if (flight.getRoute().toLowerCase().contains(destination)) {
                    botResponse += String.format("   - Local: %s (Base Fare: ₱%.2f, Status: %s)\n",
                            flight.getRoute(), flight.getBaseFare(), flight.getCurrentStatus());
                    found = true;
                }
            }
            for (Flight flight : intDbFlights) {
                if (flight.getRoute().toLowerCase().contains(destination)) {
                    botResponse += String.format("   - International: %s (Base Fare: ₱%.2f, Status: %s)\n",
                            flight.getRoute(), flight.getBaseFare(), flight.getCurrentStatus());
                    found = true;
                }
            }
            if (!found) botResponse += "   Sorry, no direct flights found for '" + destination + "' in our current list.";

        } else if (userInput.startsWith("booking status")) {
            String pnr = userInput.substring(userInput.indexOf("booking status") + "booking status".length()).trim().toUpperCase();
            Booking b = DatabaseManager.getBookingByPnr(pnr);
            if (b != null) {
                Flight f = DatabaseManager.getFlightById(b.getFlight().getId());
                botResponse += "Status for PNR " + pnr + ":\n" +
                        "   Flight: " + b.getFlight().getRoute() + "\n" +
                        "   Seats: " + String.join(", ", b.getSelectedSeats()) + "\n" +
                        "   Total Price: ₱" + b.getTotalPrice() + "\n" +
                        "   Checked In: " + (b.isCheckedIn() ? "Yes" : "No") + "\n" +
                        "   Flight Status: " + (f != null ? f.getCurrentStatus() : "N/A");
            } else {
                botResponse += "Sorry, PNR " + pnr + " not found.";
            }
        } else if (userInput.startsWith("cancel booking")) {
            String pnr = userInput.substring(userInput.indexOf("cancel booking") + "cancel booking".length()).trim().toUpperCase();
            Booking b = DatabaseManager.getBookingByPnr(pnr);
            if (b != null && loggedInUser != null && b.getUser().getUserId() == loggedInUser.getUserId()) {
                botResponse += "To cancel booking PNR " + pnr + ", please go to 'My Bookings' and use the cancel option. This ensures proper confirmation.";
            } else if (b == null) {
                botResponse += "Sorry, PNR " + pnr + " not found.";
            } else {
                botResponse += "You can only manage your own bookings. Please ensure you are logged in with the correct account.";
            }
        } else if (userInput.contains("thank you") || userInput.contains("thanks")) {
            botResponse += "You're welcome! Is there anything else?";
        } else if (userInput.contains("help")) {
            botResponse += "I can help you with:\n" +
                    "   - Finding flights (e.g., 'flights to Tokyo')\n" +
                    "   - Checking booking status (e.g., 'booking status ABC123')\n" +
                    "   - Information on how to cancel bookings (e.g., 'cancel booking XYZ789')\n";
        } else {
            botResponse += "I'm sorry, I didn't quite understand that. Can you please rephrase or type 'help' for options?";
        }

        chatbotArea.append(botResponse + "\n\n");
        chatbotArea.setCaretPosition(chatbotArea.getDocument().getLength());
    }


    private JButton createStyledButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                new EmptyBorder(8, 15, 8, 15)
        ));
        button.addMouseListener(new MouseAdapter() {
            Color originalBg = button.getBackground();
            public void mouseEntered(MouseEvent evt) { button.setBackground(originalBg.brighter()); }
            public void mouseExited(MouseEvent evt) { button.setBackground(originalBg); }
        });
        return button;
    }

    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        addAuditLog("Dark mode " + (isDarkMode ? "enabled" : "disabled") + " by " + (loggedInUser != null ? loggedInUser.getUsername() : "System"));
        try {
            if (isDarkMode) {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } else {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            SwingUtilities.updateComponentTreeUI(frame);
        } catch (Exception ex) {
            System.err.println("Failed to set LookAndFeel: " + ex);
        }
        JOptionPane.showMessageDialog(frame, "Dark Mode " + (isDarkMode ? "ON" : "OFF") + ".\nUI components will attempt to update.", "Theme Change", JOptionPane.INFORMATION_MESSAGE);
    }

    public static class User {
        private int userId;
        String username;
        String password;
        Role role;
        Set<String> bookingPNRs = new HashSet<>();
        int loyaltyPoints = 0;

        User(int userId, String u, String p, Role r) {
            this.userId = userId;
            this.username = u;
            this.password = p;
            this.role = r;
        }
        public int getUserId() { return userId; }
        public String getUsername() { return username; }
        public Role getRole() { return role; }
        public Set<String> getBookingPNRs() { return Collections.unmodifiableSet(bookingPNRs); }
        public void addBookingPNR(String pnr) { bookingPNRs.add(pnr); }
        public void removeBookingPNR(String pnr) { bookingPNRs.remove(pnr); }
        public void setBookingPNRs(Set<String> pnrs) { this.bookingPNRs = new HashSet<>(pnrs); }


        public void addLoyaltyPoints(int pts) {
            this.loyaltyPoints += pts;
            if (this.loyaltyPoints < 0) this.loyaltyPoints = 0;
        }
        public int getLoyaltyPoints() { return loyaltyPoints; }
        public void setLoyaltyPoints(int points) { this.loyaltyPoints = points; }

    }

    public static class Flight {
        private int id;
        String route;
        double baseFare;
        int totalSeats;
        Set<String> reservedSeats = new HashSet<>();
        FlightStatus currentStatus = FlightStatus.ON_TIME;


        Flight(int id, String r, double fare, int seats) {
            this.id = id;
            this.route = r;
            this.baseFare = fare;
            this.totalSeats = seats;
        }

        public int getId() { return id; }
        public String getRoute() { return route; }
        public double getBaseFare() { return baseFare; }
        public int getTotalSeats() { return totalSeats; }
        public FlightStatus getCurrentStatus() { return currentStatus; }
        public void setCurrentStatus(FlightStatus status) { this.currentStatus = status; }

        public Set<String> getReservedSeats() { return Collections.unmodifiableSet(reservedSeats); }
        public void setReservedSeats(Set<String> seats) { this.reservedSeats = new HashSet<>(seats); }

        public boolean isSeatReserved(String seatId) { return reservedSeats.contains(seatId); }
        public boolean areSeatsAvailable(Set<String> seatsToCheck) {
            for (String s : seatsToCheck) {
                if (reservedSeats.contains(s)) return false;
            }
            return true;
        }
        public void reserveSeatsOnObject(Set<String> seatsToReserve) { reservedSeats.addAll(seatsToReserve); }
        public void releaseSeatsOnObject(Set<String> seatsToRelease) { reservedSeats.removeAll(seatsToRelease); }
        public int getAvailableSeats() { return totalSeats - reservedSeats.size(); }
    }

    public static class Fare {
        String classType;
        double multiplier;
        Fare(String ct, double m) { classType=ct; multiplier=m; }
        public String getClassType() { return classType; }
        public double getMultiplier() { return multiplier; }
    }

    public static class Booking {
        private int id;
        String pnr;
        User user;
        Flight flight;
        Fare fare;
        double totalPrice;
        Set<String> selectedSeats;
        boolean isCheckedIn;
        Date bookingDate;
        int numPersons;
        String paymentMethod;


        Booking(int id, String pnr, User u, Flight f, Fare fr, double price, Set<String> seats,
                boolean checkedIn, Date bd, int numPersons, String paymentMethod) {
            this.id = id;
            this.pnr = pnr;
            this.user = u;
            this.flight = f;
            this.fare = fr;
            this.totalPrice = price;
            this.selectedSeats = new HashSet<>(seats);
            this.isCheckedIn = checkedIn;
            this.bookingDate = bd;
            this.numPersons = numPersons;
            this.paymentMethod = paymentMethod;
        }

        public int getId() { return id; }
        public String getPnr() { return pnr; }
        public User getUser() { return user; }
        public Flight getFlight() { return flight; }
        public Fare getFare() { return fare; }
        public double getTotalPrice() { return totalPrice; }
        public Set<String> getSelectedSeats() { return Collections.unmodifiableSet(selectedSeats); }
        public boolean isCheckedIn() { return isCheckedIn; }
        public void setCheckedIn(boolean checkedIn) { this.isCheckedIn = checkedIn; }
        public Date getBookingDate() { return bookingDate; }
        public int getNumPersons() { return numPersons; }
        public String getPaymentMethod() {return paymentMethod; }
    }
}