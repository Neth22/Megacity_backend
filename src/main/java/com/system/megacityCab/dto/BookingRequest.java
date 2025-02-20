package com.system.megacityCab.dto;

import lombok.Data;

@Data
public class BookingRequest {
    private String customerId;
    private String carId;
    private String pickupLocation;
    private String dropLocation;
    private String pickupDate;
    private boolean driverRequired;
}
