package com.system.megacityCab.dto;

import lombok.Data;

@Data
public class BookingRequest {
    private String bookingId;
    private String customerId;
    private String carId;
    private String pickupDate;
    private String pickupTime;
    private String pickupLocation;
    private String destination;
    private double totalAmount;
    private boolean driverRequired;
    private double distance;
    private double distanceFare;
    private double tax;
    private double driverFee;
    private String status;
}
