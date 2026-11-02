CREATE DATABASE IF NOT EXISTS library_coursework
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE library_coursework;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS loan;
DROP TABLE IF EXISTS booking;
DROP TABLE IF EXISTS reader;
DROP TABLE IF EXISTS publication;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE publication (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  isbn VARCHAR(20) NOT NULL UNIQUE,
  title VARCHAR(255) NOT NULL,
  author VARCHAR(255) NOT NULL,
  publisher VARCHAR(255),
  publication_year SMALLINT NOT NULL,
  category VARCHAR(120),
  total_copies INT NOT NULL,
  available_copies INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_publication_year CHECK (publication_year BETWEEN 1400 AND 2200),
  CONSTRAINT chk_publication_copies CHECK (total_copies >= 0 AND available_copies BETWEEN 0 AND total_copies)
) ENGINE=InnoDB;

CREATE TABLE reader (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  card_number VARCHAR(40) NOT NULL UNIQUE,
  full_name VARCHAR(180) NOT NULL,
  email VARCHAR(180),
  phone VARCHAR(40),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE booking (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  publication_id BIGINT NOT NULL,
  reader_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  state ENUM('CREATED','CONFIRMED','READY_FOR_ISSUE','COMPLETED') NOT NULL DEFAULT 'CREATED',
  f1_wait_score DECIMAL(5,2) NOT NULL DEFAULT 0,
  f2_demand DECIMAL(5,2) NOT NULL DEFAULT 0,
  f3_shortage DECIMAL(5,2) NOT NULL DEFAULT 0,
  integral_index DECIMAL(5,2) NOT NULL DEFAULT 0,
  CONSTRAINT fk_booking_publication FOREIGN KEY (publication_id) REFERENCES publication(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_booking_reader FOREIGN KEY (reader_id) REFERENCES reader(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_booking_factors CHECK (f1_wait_score BETWEEN 0 AND 100 AND f2_demand BETWEEN 0 AND 100 AND f3_shortage BETWEEN 0 AND 100)
) ENGINE=InnoDB;

CREATE TABLE loan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_id BIGINT NOT NULL UNIQUE,
  issued_at DATE NOT NULL,
  due_date DATE NOT NULL,
  returned_at DATE,
  CONSTRAINT fk_loan_booking FOREIGN KEY (booking_id) REFERENCES booking(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_loan_dates CHECK (due_date >= issued_at AND (returned_at IS NULL OR returned_at >= issued_at))
) ENGINE=InnoDB;

INSERT INTO publication (isbn, title, author, publisher, publication_year, category, total_copies, available_copies) VALUES
('9786178012345', 'Кобзар', 'Тарас Шевченко', 'Основи', 2021, 'Поезія', 5, 5),
('9786178023456', 'Лісова пісня', 'Леся Українка', 'Фоліо', 2020, 'Драма', 3, 3),
('9786178034567', 'Тигролови', 'Іван Багряний', 'КСД', 2019, 'Роман', 2, 2);

INSERT INTO reader (card_number, full_name, email, phone) VALUES
('LIB-2026-001', 'Олена Коваль', 'olena.koval@example.com', '+380501112233'),
('LIB-2026-002', 'Андрій Мельник', 'andrii.melnyk@example.com', '+380671234567');

INSERT INTO booking (publication_id, reader_id, state, created_at) VALUES
(1, 1, 'CREATED', CURRENT_TIMESTAMP),
(2, 2, 'CONFIRMED', CURRENT_TIMESTAMP);
