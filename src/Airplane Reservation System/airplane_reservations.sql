-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: May 23, 2025 at 08:13 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `airplane_reservations`
--

-- --------------------------------------------------------

--
-- Table structure for table `audit_log`
--

CREATE TABLE `audit_log` (
  `log_id` int(11) NOT NULL,
  `log_timestamp` timestamp NOT NULL DEFAULT current_timestamp(),
  `message` text NOT NULL,
  `username` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `audit_log`
--

INSERT INTO `audit_log` (`log_id`, `log_timestamp`, `message`, `username`) VALUES
(1, '2025-05-23 17:52:04', 'System Initialized: DB Connection OK.', 'System'),
(2, '2025-05-23 17:52:04', 'System UI Initialized.', 'System'),
(3, '2025-05-23 17:52:24', 'New user registered: seph', 'System'),
(4, '2025-05-23 17:52:29', 'User logged in: seph', 'seph'),
(5, '2025-05-23 17:53:08', 'Booking completed. PNR: A6HTEW, User: seph, Flight: Cebu to Manila (CEB-MNL), Amount: 3150.0, Payment: GCash', 'seph'),
(6, '2025-05-23 17:57:09', 'System Shutdown Initiated.', 'seph'),
(7, '2025-05-23 18:05:06', 'System Initialized: DB Connection OK.', 'System'),
(8, '2025-05-23 18:05:06', 'System UI Initialized.', 'System'),
(9, '2025-05-23 18:05:09', 'System Shutdown Initiated.', 'System'),
(10, '2025-05-23 18:05:21', 'System Initialized: DB Connection OK.', 'System'),
(11, '2025-05-23 18:05:22', 'System UI Initialized.', 'System'),
(12, '2025-05-23 18:05:28', 'User logged in: seph', 'seph'),
(13, '2025-05-23 18:05:32', 'System Shutdown Initiated.', 'seph'),
(14, '2025-05-23 18:06:40', 'System Initialized: DB Connection OK.', 'System'),
(15, '2025-05-23 18:06:41', 'System UI Initialized.', 'System'),
(16, '2025-05-23 18:06:47', 'User logged in: admin', 'admin'),
(17, '2025-05-23 18:06:57', 'User logged out: admin', 'admin'),
(18, '2025-05-23 18:07:01', 'User logged in: seph', 'seph'),
(19, '2025-05-23 18:07:09', 'System Shutdown Initiated.', 'seph');

-- --------------------------------------------------------

--
-- Table structure for table `bookings`
--

CREATE TABLE `bookings` (
  `booking_id` int(11) NOT NULL,
  `pnr` varchar(10) NOT NULL,
  `user_id` int(11) NOT NULL,
  `flight_id` int(11) NOT NULL,
  `fare_class` varchar(50) NOT NULL,
  `fare_multiplier` decimal(3,1) NOT NULL,
  `total_price` decimal(10,2) NOT NULL,
  `num_persons` int(11) NOT NULL,
  `booking_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `is_checked_in` tinyint(1) DEFAULT 0,
  `payment_method` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `bookings`
--

INSERT INTO `bookings` (`booking_id`, `pnr`, `user_id`, `flight_id`, `fare_class`, `fare_multiplier`, `total_price`, `num_persons`, `booking_date`, `is_checked_in`, `payment_method`) VALUES
(1, 'A6HTEW', 4, 7, 'Economy', 1.0, 3150.00, 1, '2025-05-23 17:53:08', 0, 'GCash');

-- --------------------------------------------------------

--
-- Table structure for table `flights`
--

CREATE TABLE `flights` (
  `flight_id` int(11) NOT NULL,
  `route` varchar(100) NOT NULL,
  `base_fare` decimal(10,2) NOT NULL,
  `total_seats` int(11) NOT NULL,
  `flight_type` enum('LOCAL','INTERNATIONAL') NOT NULL,
  `current_status` enum('ON_TIME','DELAYED','CANCELED') DEFAULT 'ON_TIME',
  `departure_time` timestamp NULL DEFAULT NULL,
  `arrival_time` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `flights`
--

INSERT INTO `flights` (`flight_id`, `route`, `base_fare`, `total_seats`, `flight_type`, `current_status`, `departure_time`, `arrival_time`) VALUES
(1, 'Manila to Cebu (MNL-CEB)', 3200.00, 150, 'LOCAL', 'ON_TIME', NULL, NULL),
(2, 'Manila to Davao (MNL-DVO)', 3500.00, 160, 'LOCAL', 'ON_TIME', NULL, NULL),
(3, 'Manila to Iloilo (MNL-ILO)', 3100.00, 140, 'LOCAL', 'ON_TIME', NULL, NULL),
(4, 'Manila to Bacolod (MNL-BCD)', 3300.00, 130, 'LOCAL', 'ON_TIME', NULL, NULL),
(5, 'Manila to Bohol (MNL-TAG)', 3600.00, 150, 'LOCAL', 'ON_TIME', NULL, NULL),
(6, 'Manila to Zamboanga (MNL-ZAM)', 3500.00, 140, 'LOCAL', 'ON_TIME', NULL, NULL),
(7, 'Cebu to Manila (CEB-MNL)', 3150.00, 150, 'LOCAL', 'ON_TIME', NULL, NULL),
(8, 'Davao to Manila (DVO-MNL)', 3450.00, 160, 'LOCAL', 'DELAYED', NULL, NULL),
(9, 'Manila to Tokyo (MNL-NRT)', 12000.00, 200, 'INTERNATIONAL', 'ON_TIME', NULL, NULL),
(10, 'Manila to Seoul (MNL-ICN)', 11000.00, 180, 'INTERNATIONAL', 'ON_TIME', NULL, NULL),
(11, 'Manila to Singapore (MNL-SIN)', 10000.00, 180, 'INTERNATIONAL', 'ON_TIME', NULL, NULL),
(12, 'Manila to Dubai (MNL-DXB)', 18000.00, 200, 'INTERNATIONAL', 'ON_TIME', NULL, NULL),
(13, 'Manila to Paris (MNL-CDG)', 19000.00, 210, 'INTERNATIONAL', 'ON_TIME', NULL, NULL),
(14, 'Manila to New York (MNL-JFK)', 20000.00, 220, 'INTERNATIONAL', 'ON_TIME', NULL, NULL),
(15, 'Tokyo to Manila (NRT-MNL)', 11800.00, 200, 'INTERNATIONAL', 'ON_TIME', NULL, NULL),
(16, 'Seoul to Manila (ICN-MNL)', 10800.00, 180, 'INTERNATIONAL', 'DELAYED', NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `reserved_seats`
--

CREATE TABLE `reserved_seats` (
  `reserved_seat_id` int(11) NOT NULL,
  `booking_id` int(11) NOT NULL,
  `flight_id` int(11) NOT NULL,
  `seat_id_str` varchar(5) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `reserved_seats`
--

INSERT INTO `reserved_seats` (`reserved_seat_id`, `booking_id`, `flight_id`, `seat_id_str`) VALUES
(1, 1, 7, '4C');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `user_id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('ADMIN','CUSTOMER') NOT NULL,
  `loyalty_points` int(11) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`user_id`, `username`, `password`, `role`, `loyalty_points`, `created_at`) VALUES
(1, 'admin', 'admin123', 'ADMIN', 0, '2025-05-23 17:30:06'),
(2, 'user1', 'pass1', 'CUSTOMER', 0, '2025-05-23 17:30:06'),
(3, 'john', 'doe', 'CUSTOMER', 0, '2025-05-23 17:30:06'),
(4, 'seph', '123456', 'CUSTOMER', 0, '2025-05-23 17:52:24');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `audit_log`
--
ALTER TABLE `audit_log`
  ADD PRIMARY KEY (`log_id`);

--
-- Indexes for table `bookings`
--
ALTER TABLE `bookings`
  ADD PRIMARY KEY (`booking_id`),
  ADD UNIQUE KEY `pnr` (`pnr`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `flight_id` (`flight_id`);

--
-- Indexes for table `flights`
--
ALTER TABLE `flights`
  ADD PRIMARY KEY (`flight_id`),
  ADD UNIQUE KEY `route` (`route`);

--
-- Indexes for table `reserved_seats`
--
ALTER TABLE `reserved_seats`
  ADD PRIMARY KEY (`reserved_seat_id`),
  ADD UNIQUE KEY `unique_flight_seat` (`flight_id`,`seat_id_str`),
  ADD KEY `booking_id` (`booking_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `username` (`username`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `audit_log`
--
ALTER TABLE `audit_log`
  MODIFY `log_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=20;

--
-- AUTO_INCREMENT for table `bookings`
--
ALTER TABLE `bookings`
  MODIFY `booking_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `flights`
--
ALTER TABLE `flights`
  MODIFY `flight_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=17;

--
-- AUTO_INCREMENT for table `reserved_seats`
--
ALTER TABLE `reserved_seats`
  MODIFY `reserved_seat_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `bookings`
--
ALTER TABLE `bookings`
  ADD CONSTRAINT `bookings_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  ADD CONSTRAINT `bookings_ibfk_2` FOREIGN KEY (`flight_id`) REFERENCES `flights` (`flight_id`);

--
-- Constraints for table `reserved_seats`
--
ALTER TABLE `reserved_seats`
  ADD CONSTRAINT `reserved_seats_ibfk_1` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`booking_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `reserved_seats_ibfk_2` FOREIGN KEY (`flight_id`) REFERENCES `flights` (`flight_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
