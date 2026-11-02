package ua.edu.nupp.library.model;

import java.time.LocalDateTime;

public record Booking(Long id, long publicationId, long readerId, LocalDateTime createdAt,
                      BookingState state, double f1WaitScore, double f2Demand,
                      double f3Shortage, double integralIndex) { }
