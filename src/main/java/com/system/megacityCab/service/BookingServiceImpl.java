package com.system.megacityCab.service;


import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.system.megacityCab.dto.BookingRequest;
import com.system.megacityCab.dto.CancellationRequest;
import com.system.megacityCab.exception.InvalidBookingException;
import com.system.megacityCab.exception.ResourceNotFoundException;
import com.system.megacityCab.exception.UnauthorizedException;
import com.system.megacityCab.model.Booking;
import com.system.megacityCab.model.BookingStatus;
import com.system.megacityCab.model.Car;
import com.system.megacityCab.model.Driver;
import com.system.megacityCab.repository.BookingRepository;
import com.system.megacityCab.repository.CarRepository;
import com.system.megacityCab.repository.CustomerRepository;
import com.system.megacityCab.repository.DriverRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BookingServiceImpl implements BookingService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private static final int CANCELLATION_WINDOW_HOURS = 24;
    private static final double CANCELLATION_FEE_PERCENTAGE = 0.1;

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();

    }

    @Override
    public Booking getBookingById(String bookingId) {
        return bookingRepository.findById(bookingId).orElse(null);
    }

    @Override
    @Transactional
    public Booking createBooking(BookingRequest request) {

        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new ResourceNotFoundException("Car not found"));
        if (!isCarAvailableForTime(car, request.getPickupDate())) {
            throw new InvalidBookingException("Car is not available for requested time");

        }

        Booking booking = new Booking();
        booking.setCustomerId(request.getCustomerId());
        booking.setCarId(request.getCarId());
        booking.setPickupLocation(request.getPickupLocation());
        booking.setDropLocation(request.getDropLocation());
        booking.setPickupDate(request.getPickupDate());
        booking.setBookingDate(LocalDateTime.now().format(DATE_FORMATTER));
        booking.setDriverRequired(request.isDriverRequired());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalAmount(calculateBookingAmount(car, request));

        if (request.isDriverRequired()) {
            assignDriverToBooking(booking, car);

        }

        car.setAvailable(false);
        carRepository.save(car);

        log.info("Create new booking with ID :  {} for customer : {}",
                booking.getBookingId(), booking.getCustomerId());
        return bookingRepository.save(booking);

    }

    @Transactional
    public Booking cancelBooking(String customerId, CancellationRequest request) {

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getCustomerId().equals(customerId)) {

            throw new UnauthorizedException("Not Authorized to cancel booking");

        }

        if (!booking.canBeCancelled()) {
            throw new InvalidBookingException("Booking can not be  cancelled in current situation");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(request.getReason());
        booking.setCancellationTime(LocalDateTime.now().format(DATE_FORMATTER));

        releaseBookingResource(booking);
        handleCancellationRefund(booking);

        log.info("Cancelled booking with ID: {} for customer: {}",
                booking.getBookingId(), booking.getCustomerId()

        );

        return bookingRepository.save(booking);

    }

    @Transactional(readOnly = true)
    public List<Booking> getCustomerBookings(String customerId) {
        return bookingRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public Booking getBookingDetails(String customerId, String bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getCustomerId().equals(customerId)) {

            throw new UnauthorizedException("Not authorized to view this booking");
        }

        return booking;
    }

    @Override
    public void deleteBooking(String bookingId, String customerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getCustomerId().equals(customerId)) {
            throw new UnauthorizedException("Not authorized to delete this booking ");
        }

        if (!booking.canBeDeleted()) {
            throw new InvalidBookingException("Booking can't delete in current state");
        }

        releaseBookingResource(booking);

        bookingRepository.delete(booking);
        log.info("Deleted booking with ID : {} for customer: {} ", bookingId, customerId);
    }

    private boolean isCarAvailableForTime(Car car, String requestedTime) {

        if (!car.isAvailable()) {
            return false;
        }

        List<Booking> existingBookings = bookingRepository.findByCarIdAndStatus(

                car.getCarId(),
                BookingStatus.CONFIRMED

        );

        return existingBookings.stream()
                .noneMatch(booking -> isTimeOverlapping(booking.getPickupDate(), requestedTime));

    }

    private boolean isTimeOverlapping(String existing, String requested) {
        LocalDateTime existingTime = LocalDateTime.parse(existing, DATE_FORMATTER);
        LocalDateTime requestedTime = LocalDateTime.parse(requested, DATE_FORMATTER);

        Duration buffer = Duration.ofHours(1);
        return Math.abs(Duration.between(existingTime, requestedTime).toHours()) < buffer.toHours();

    }

    private double calculateBookingAmount(Car car, BookingRequest request) {
        double baseAmount = car.getBaseRate();
        if (request.isDriverRequired()) {
            baseAmount += car.getDriverRate();
        }

        return baseAmount;
    }

    private void assignDriverToBooking(Booking booking, Car car) {

        Driver driver;

        if (car.getAssignedDriverId() != null) {
            driver = driverRepository.findById(car.getAssignedDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned driver  not found"));

            if (!driver.isAvailable()) {
                throw new InvalidBookingException("Car's assigned driver is not available");
            }
        } else {
            driver = driverRepository.findFirstByAvailableAndHasOwnCarFalse(true)
                    .orElseThrow(() -> new ResourceNotFoundException("No available driver"));
        }

        booking.setDriverId(driver.getDriverId());
        driver.setAvailable(false);
        driverRepository.save(driver);
        log.info("Assign driver {} to booking {}", driver.getDriverId(), booking.getBookingId());

    }

    private void releaseBookingResource(Booking booking) {

        if (booking.getCarId() != null) {

            Car car = carRepository.findById(booking.getCarId()).orElse(null);
            if (car != null && !car.isAvailable()) {
                car.setAvailable(true);
                carRepository.save(car);
                log.info("Released car {} from booking {} ", car.getCarId(), booking.getBookingId());
            }
        }

        if (booking.getDriverId() != null) {
            Driver driver = driverRepository.findById(booking.getDriverId()).orElse(null);

            if (driver != null && !driver.isAvailable()) {

                driver.setAvailable(true);
                driverRepository.save(driver);
                log.info("Released driver {} from booking {}", booking.getBookingId(), driver.getDriverId());
            }
        }
    }

    private void handleCancellationRefund(Booking booking) {

        LocalDateTime pickupDateTime = LocalDateTime.parse(booking.getPickupDate(), DATE_FORMATTER);
        LocalDateTime cancellationDeadline = pickupDateTime.minusHours(CANCELLATION_WINDOW_HOURS);
        if (LocalDateTime.now().isBefore(cancellationDeadline)) {

            booking.setRefundAmount(booking.getTotalAmount());
        } else {
            double cancellationFee = booking.getTotalAmount() * CANCELLATION_FEE_PERCENTAGE;
            booking.setRefundAmount(booking.getTotalAmount() - cancellationFee);
        }
        log.info("Processing refund of {} for booking {}",
                booking.getRefundAmount(), booking.getBookingId());
    }

    @Scheduled(fixedRate = 10000)
    public void checkAndUpdateCarAvailability() {
        String currentTime = LocalDateTime.now().format(DATE_FORMATTER);
        List<Booking> activeBookings = bookingRepository.findByStatusAndPickupDateBefore(
                BookingStatus.CONFIRMED, currentTime);

        for (Booking booking : activeBookings) {
            updateBookingStatus(booking);
        }

        log.info("Completed periodic booking status check ");

    }

    private void updateBookingStatus(Booking booking) {

        LocalDateTime pickupTime = LocalDateTime.parse(booking.getPickupDate(), DATE_FORMATTER);
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(pickupTime)) {

            booking.setStatus(BookingStatus.IN_PROGRESS);
            bookingRepository.save(booking);
            log.info("updated booking {} status to IN_PROGRESS", booking.getBookingId());

        }

    }

}
