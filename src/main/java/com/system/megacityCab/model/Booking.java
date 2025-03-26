package com.system.megacityCab.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "bookings")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Booking {

    @Id
    private String bookingId;

    private String customerId;

    private String customerEmail;

    private String carId;

    private String driverId;

    private String pickupLocation;

    private String destination;

    private String bookingDate;

    private String pickupDate;

    private String pickupTime;

    private double totalAmount;

    
    private double distance; 
    private double distanceFare; 
    private double tax; 
    private double driverFee; 

    @Transient
    private String passengerName;

    

    private boolean completed = false;

    private boolean driverRequired = false;

    // New field to store driver assignment status message
    private String driverAssignmentMessage;

    private BookingStatus status = BookingStatus.PENDING;

    private String cancellationReason;

    private String cancellationTime;

    private boolean refundIssued = false;

    private double refundAmount;

    @Transient
    private Driver driverDetails;

    public boolean canBeCancelled() {
        return status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED;
    }

    public boolean canBeDeleted() {
        return status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED;
    }
}