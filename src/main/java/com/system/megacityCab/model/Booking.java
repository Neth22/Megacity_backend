package com.system.megacityCab.model;



import org.springframework.data.annotation.Id;
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
    private String driverId;
    private String carId;


    private String pickupDate;
    private String pickupLocation;
    private String dropLocation;
    private double tax;
    private double totalAmount;
    private String bookingDate;

    private boolean completed = false;
    private boolean driverRequired = false;

    private BookingStatus status = BookingStatus.PENDING;
    private String cancellationReason;
    private String cancellationTime;

    private boolean refundIssued = false;

    private double refundAmount;
    
    public boolean canBeCancelled(){
        return status== BookingStatus.PENDING || status == BookingStatus.CONFIRMED;
    }

    public boolean canBeDeleted(){
        return status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED;
    }

    

}
