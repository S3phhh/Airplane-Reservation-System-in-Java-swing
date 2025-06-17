# Airplane Reservation System

A Java Swing-based desktop application for booking airplane flights with real-time seat selection, pricing, and MySQL database integration. This system is designed for local airline simulations or academic projects.

## ✈️ Features

- User and Admin login system
- Book local and international flights
- Seat selection panel (Economy, Economy+, Business)
- Price calculation based on seat type and passengers
- MySQL integration for storing and managing bookings
- Dark mode toggle, modern GUI styling
- Error handling and sound effects
- Booking requests, real-time flight status updates
- Ticket issuing and printable receipts

## 🛠️ Technologies Used

- Java (Swing GUI)
- MySQL (via JDBC)
- XAMPP / phpMyAdmin for local DB management

## 📂 Project Structure

```
Airplane-Reservation-System/
├── src/               # Java source files
├── db/                # SQL file for database schema
├── assets/            # Images, icons, sound effects
├── lib/               # JDBC connectors
├── README.md
└── Airplane.jar       # Compiled runnable JAR (if available)
```

## ⚙️ How to Run

1. **Install XAMPP** and start MySQL.
2. **Import `db/airplane.sql`** into phpMyAdmin.
3. **Compile and run `Airplane.java`** using your IDE or terminal:
   ```bash
   javac -cp .;lib/mysql-connector.jar src/Airplane.java
   java -cp .;lib/mysql-connector.jar src.Airplane
   ```

## 👨‍💻 Author

Joseph Nathaniel C. Unias  
🏫  Our Lady of Fatima University  
📧 uniasjosephnathaniel@gmail.com

---

## 📌 Note

This project is for educational purposes. You may modify and enhance it for your specific needs (e.g., online booking support, print-ready tickets, better UI).
