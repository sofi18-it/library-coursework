package ua.edu.nupp.library.service;

import java.time.Duration;
import java.time.LocalDateTime;

public record IntegralScore(double f1,double f2,double f3,double integral) {
    public static IntegralScore calculate(LocalDateTime createdAt,long activeBookings,int totalCopies,int availableCopies,LocalDateTime now) {
        double f1=clamp(Duration.between(createdAt,now).toDays()*100.0/30.0);
        double f2=activeBookings<=0?0:(totalCopies<=0?100:clamp(100.0*activeBookings/totalCopies));
        double f3=totalCopies<=0?100:clamp(100.0*Math.max(0,activeBookings-availableCopies)/totalCopies);
        return new IntegralScore(f1,f2,f3,0.40*f1+0.35*f2+0.25*f3);
    }
    private static double clamp(double value){return Math.max(0,Math.min(100,value));}
}
