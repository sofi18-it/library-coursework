package ua.edu.nupp.library.client.model;
import java.time.LocalDate;
public record Loan(Long id,long bookingId,LocalDate issuedAt,LocalDate dueDate,LocalDate returnedAt) { }
