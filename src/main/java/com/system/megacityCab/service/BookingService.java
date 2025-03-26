package com.system.megacityCab.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.system.megacityCab.dto.BookingRequest;
import com.system.megacityCab.dto.CancellationRequest;
import com.system.megacityCab.model.Booking;

@Service
public interface BookingService {


    List<Booking> getAllBookings();
    Booking getBookingById(String bookingId);
    Booking createBooking(BookingRequest request);
    Booking cancelBooking(String customerId, CancellationRequest request);
    List<Booking> getCustomerBookings(String customerId);
    Booking getBookingDetails(String customerId, String bookingId);
    void deleteBooking(String customerId, String bookingId);
    Booking confirmBooking(String bookingId);
    List<Booking> getAvailableBookings();
    boolean hasBookingWithDriver(String customerEmail, String driverId);
    
}
