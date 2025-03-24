package com.system.megacityCab.controller;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.system.megacityCab.dto.BookingRequest;
import com.system.megacityCab.dto.CancellationRequest;
import com.system.megacityCab.exception.InvalidBookingException;
import com.system.megacityCab.exception.InvalidBookingStateException;
import com.system.megacityCab.exception.ResourceNotFoundException;
import com.system.megacityCab.exception.UnauthorizedException;
import com.system.megacityCab.model.Booking;
import com.system.megacityCab.model.Customer;
import com.system.megacityCab.model.Driver;
import com.system.megacityCab.repository.BookingRepository;
import com.system.megacityCab.repository.CustomerRepository;
import com.system.megacityCab.repository.DriverRepository;
import com.system.megacityCab.service.BookingService;

import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/auth/bookings")
@Slf4j
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private Booking enrichBooking(Booking booking) {
        Customer customer = customerRepository.findById(booking.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + booking.getCustomerId()));
        booking.setPassengerName(customer.getCustomerName());
        
        return booking;
    }

    @GetMapping("/getallbookings")
    public ResponseEntity<?> getAllBookings() {
        try {
            log.info("Fetching all bookings");
            List<Booking> bookings = bookingService.getAllBookings();
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            log.error("Error fetching all bookings: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching all bookings.");
        }
    }

    @PostMapping("/createbooking")
    public ResponseEntity<?> createBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @Validated @RequestBody BookingRequest bookingRequest) {
        try {
            String email = userDetails.getUsername();
            log.info("Creating new booking for customer email: {}", email);

            // Fetch customer by email
            Customer customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + email));

            // Validate required fields
            if (bookingRequest.getCarId() == null || bookingRequest.getPickupLocation() == null ||
                    bookingRequest.getDestination() == null || bookingRequest.getPickupDate() == null ||
                    bookingRequest.getPickupTime() == null || bookingRequest.getDistance() < 0) {
                throw new IllegalArgumentException("Missing or invalid required fields in booking request.");
            }

            // Generate bookingId if not provided
            if (bookingRequest.getBookingId() == null || bookingRequest.getBookingId().isEmpty()) {
                bookingRequest.setBookingId(UUID.randomUUID().toString());
            }

            // Set customerId in the request
            bookingRequest.setCustomerId(customer.getCustomerId());

            // Create the booking
            Booking booking = bookingService.createBooking(bookingRequest);
            log.info("Booking created successfully with ID: {}", booking.getBookingId());
            return ResponseEntity.status(HttpStatus.CREATED).body(booking);
        } catch (ResourceNotFoundException e) {
            log.error("Resource not found while creating booking: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InvalidBookingException e) {
            log.error("Invalid booking request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Validation error in booking request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating booking: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while creating the booking.");
        }
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String bookingId,
            @Validated @RequestBody CancellationRequest cancellationRequest) {
        try {
            String email = userDetails.getUsername();
            log.info("Cancelling booking: {} for customer email: {}", bookingId, email);

            // Fetch customer by email
            Customer customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + email));

            // Validate cancellation request
            if (cancellationRequest.getReason() == null || cancellationRequest.getReason().isEmpty()) {
                throw new IllegalArgumentException("Cancellation reason is required.");
            }

            // Set bookingId in the cancellation request
            cancellationRequest.setBookingId(bookingId);

            // Cancel the booking
            Booking cancelledBooking = bookingService.cancelBooking(customer.getCustomerId(), cancellationRequest);
            log.info("Booking cancelled successfully with ID: {}", cancelledBooking.getBookingId());
            return ResponseEntity.ok(cancelledBooking);
        } catch (ResourceNotFoundException e) {
            log.error("Booking not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (UnauthorizedException e) {
            log.error("Unauthorized cancellation attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (InvalidBookingStateException e) {
            log.error("Invalid booking state for cancellation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Validation error in cancellation request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error cancelling booking: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while cancelling the booking.");
        }
    }

    @GetMapping("/getallcustomerbookings")
    public ResponseEntity<?> getCustomerBookings(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                log.error("No authenticated user found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
            }

            String email = userDetails.getUsername();
            log.info("Fetching bookings for customer email: {}", email);

            Customer customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + email));

            List<Booking> bookings = bookingService.getCustomerBookings(customer.getCustomerId());
            return ResponseEntity.ok(bookings.isEmpty() ? Collections.emptyList() : bookings);
        } catch (ResourceNotFoundException e) {
            log.error("Customer not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching customer bookings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Detailed error: " + e.getMessage());
        }
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getCustomerBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String bookingId) {
        try {
            String email = userDetails.getUsername();
            log.info("Fetching booking: {} for customer email: {}", bookingId, email);

            // Fetch customer by email
            Customer customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + email));

            // Fetch booking details
            Booking booking = bookingService.getBookingDetails(customer.getCustomerId(), bookingId);
            return ResponseEntity.ok(booking);
        } catch (ResourceNotFoundException e) {
            log.error("Booking not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (UnauthorizedException e) {
            log.error("Unauthorized access attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching booking details: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching the booking details.");
        }
    }

    @DeleteMapping("/delete/{bookingId}")
    public ResponseEntity<?> deleteBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String bookingId) {
        try {
            String email = userDetails.getUsername();
            log.info("Deleting booking: {} for customer email: {}", bookingId, email);

            // Fetch customer by email
            Customer customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + email));

            // Delete the booking
            bookingService.deleteBooking(customer.getCustomerId(), bookingId);
            log.info("Booking deleted successfully with ID: {}", bookingId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.error("Booking not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (UnauthorizedException e) {
            log.error("Unauthorized deletion attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (InvalidBookingStateException e) {
            log.error("Invalid booking state for deletion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting booking: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while deleting the booking.");
        }
    }

    @PutMapping("/{bookingId}/confirm")
    public ResponseEntity<Booking> confirmBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String bookingId) {
        String email = userDetails.getUsername();
        log.info("Confirming booking: {} for driver email: {}", bookingId, email);

        Driver driver = driverRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with email: " + email));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getDriverId().equals(driver.getDriverId())) {
            throw new IllegalStateException("You are not authorized to confirm this booking.");
        }

        Booking confirmedBooking = bookingService.confirmBooking(bookingId);
        return ResponseEntity.ok(enrichBooking(confirmedBooking));
    }
}