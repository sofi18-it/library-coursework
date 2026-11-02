package ua.edu.nupp.library.client.model;
import java.time.LocalDateTime;
public record Booking(Long id,long publicationId,long readerId,LocalDateTime createdAt,String state,double f1WaitScore,double f2Demand,double f3Shortage,double integralIndex) { }
