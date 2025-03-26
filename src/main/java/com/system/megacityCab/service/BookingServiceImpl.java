package com.system.megacityCab.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.system.megacityCab.dto.BookingRequest;
import com.system.megacityCab.dto.CancellationRequest;
import com.system.megacityCab.exception.InvalidBookingException;
import com.system.megacityCab.exception.InvalidBookingStateException;
import com.system.megacityCab.exception.ResourceNotFoundException;
import com.system.megacityCab.exception.UnauthorizedException;
import com.system.megacityCab.model.Booking;
import com.system.megacityCab.model.BookingStatus;
import com.system.megacityCab.model.Car;
import com.system.megacityCab.model.Customer;
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
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private DriverService driverService;

    private static final int CANCELLATION_WINDOW_HOURS = 24;
    private static final double CANCELLATION_FEE_PERCENTAGE = 0.1;
    private static final double DRIVER_FEE = 50.0; // Fixed driver fee as per frontend
    private static final double DISTANCE_RATE = 1.5; // Rs. 1.5 per km, as per frontend
    private static final double TAX = 2.5; // Fixed tax as per frontend

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
        log.info("Creating booking for customer ID: {}, car ID: {}", request.getCustomerId(), request.getCarId());

        // Validate car existence and availability
        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new ResourceNotFoundException("Car not found with ID: " + request.getCarId()));

        // Check car availability based on pickup date and time
        if (!isCarAvailableForTime(car, request.getPickupDate(), request.getPickupTime())) {
            throw new InvalidBookingException("Car is not available for the requested time");
        }

        // Get current date and time
        LocalDateTime now = LocalDateTime.now();
        String currentDateStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String currentTimeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        // Check if the booking time has already passed
        boolean isPickupTimePassed = false;
        try {
            LocalDate bookingDate = LocalDate.parse(request.getPickupDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime bookingTime = LocalTime.parse(request.getPickupTime(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalDate currentDate = LocalDate.parse(currentDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime currentTime = LocalTime.parse(currentTimeStr, DateTimeFormatter.ofPattern("HH:mm"));

            isPickupTimePassed = bookingDate.isBefore(currentDate) ||
                    (bookingDate.isEqual(currentDate) && bookingTime.isBefore(currentTime));
        } catch (Exception e) {
            log.error("Error parsing dates for booking time check: {}", e.getMessage());
        }

        // Create booking
        Booking booking = new Booking();
        booking.setCustomerId(request.getCustomerId());
        booking.setCarId(request.getCarId());
        booking.setBookingId(request.getBookingId());
        booking.setPickupLocation(request.getPickupLocation());
        booking.setDestination(request.getDestination());
        booking.setPickupDate(request.getPickupDate());
        booking.setPickupTime(request.getPickupTime());
        booking.setBookingDate(LocalDateTime.now().format(DATE_FORMATTER));
        booking.setDriverRequired(request.isDriverRequired());
        booking.setStatus(BookingStatus.PENDING);

        // Set distance, distanceFare, and tax
        double distance = request.getDistance();
        booking.setDistance(distance);
        double distanceFare = distance * DISTANCE_RATE;
        booking.setDistanceFare(distanceFare);
        booking.setTax(TAX);

        // Assign driver if requested and calculate driver fee
        if (request.isDriverRequired()) {
            assignDriverToBooking(booking, car);
        } else {
            booking.setDriverFee(0.0);
            booking.setDriverAssignmentMessage("Driver not requested.");
        }

        // Calculate total amount
        double totalAmount = calculateBookingAmount(car, booking);
        booking.setTotalAmount(totalAmount);

        // Mark car as unavailable if pickup time has already passed
        if (isPickupTimePassed) {
            car.setAvailable(false);
            booking.setStatus(BookingStatus.IN_PROGRESS);
            log.info("Booking created with a past pickup time. Car {} marked as unavailable.", car.getCarId());
        }

        carRepository.save(car);
        Booking savedBooking = bookingRepository.save(booking);

        if (booking.isDriverRequired() && booking.getDriverId() != null) {
            Driver driver = driverService.getDriverById(booking.getDriverId());
            savedBooking.setDriverDetails(driver);
        }

        // Send email confirmation to customer
        sendBookingConfirmationEmail(savedBooking);

        log.info("Created new booking with ID: {} for customer: {}", booking.getBookingId(), booking.getCustomerId());
        return savedBooking;
    }

    @Override
    public Booking confirmBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking can only be confirmed from PENDING status.");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking confirmedBooking = bookingRepository.save(booking);

        // Send confirmation email when booking is confirmed
        sendBookingStatusUpdateEmail(confirmedBooking, "Booking Confirmed");

        return confirmedBooking;
    }

    @Override
    public void deleteBooking(String customerId, String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getCustomerId().equals(customerId)) {
            throw new UnauthorizedException("Not authorized to delete this booking");
        }

        if (!booking.canBeDeleted()) {
            throw new InvalidBookingStateException("Booking cannot be deleted in current state");
        }

        releaseBookingResource(booking);
        bookingRepository.delete(booking);
        log.info("Deleted booking with ID: {} for customer: {}", bookingId, customerId);
    }

    @Override
    public boolean hasBookingWithDriver(String customerEmail, String driverId) {
        return bookingRepository.existsByCustomerEmailAndDriverId(customerEmail, driverId);
    }

    private boolean isCarAvailableForTime(Car car, String requestedDate, String requestedTime) {
        if (!car.isAvailable()) {
            return false;
        }

        List<Booking> existingBookings = bookingRepository.findByCarIdAndStatus(
                car.getCarId(),
                BookingStatus.CONFIRMED);

        return existingBookings.stream()
                .noneMatch(booking -> isTimeOverlapping(booking.getPickupDate(), booking.getPickupTime(),
                        requestedDate, requestedTime));
    }

    private boolean isTimeOverlapping(String existingDate, String existingTime,
            String requestedDate, String requestedTime) {
        try {
            LocalDate existingDateObj = LocalDate.parse(existingDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime existingTimeObj = LocalTime.parse(existingTime, DateTimeFormatter.ofPattern("HH:mm"));
            LocalDate requestedDateObj = LocalDate.parse(requestedDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime requestedTimeObj = LocalTime.parse(requestedTime, DateTimeFormatter.ofPattern("HH:mm"));

            // If dates are different, no overlap
            if (!existingDateObj.isEqual(requestedDateObj)) {
                return false;
            }

            // If dates are the same, check if times are within 1 hour of each other
            LocalDateTime existingDateTime = LocalDateTime.of(existingDateObj, existingTimeObj);
            LocalDateTime requestedDateTime = LocalDateTime.of(requestedDateObj, requestedTimeObj);

            Duration buffer = Duration.ofHours(1);
            return Math.abs(Duration.between(existingDateTime, requestedDateTime).toHours()) < buffer.toHours();
        } catch (Exception e) {
            log.error("Error parsing dates for time overlap check: {}", e.getMessage());
            return false;
        }
    }

    private double calculateBookingAmount(Car car, Booking booking) {
        double baseAmount = car.getBaseRate();
        double distanceFare = booking.getDistanceFare();
        double tax = booking.getTax();
        double driverFee = booking.getDriverFee();

        double totalAmount = baseAmount + distanceFare + tax + driverFee;
        log.info(
                "Calculated booking amount: Base Fare = {}, Distance Fare ({} km) = {}, Tax = {}, Driver Fee = {}, Total = {}",
                baseAmount, booking.getDistance(), distanceFare, tax, driverFee, totalAmount);
        return totalAmount;
    }

    private void assignDriverToBooking(Booking booking, Car car) {
        try {
            Driver driver;
            if (car.getAssignedDriverId() != null) {
                driver = driverRepository.findById(car.getAssignedDriverId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Assigned driver not found for car ID: " + car.getCarId()));
                if (!driver.isAvailable()) {
                    log.warn("Car's assigned driver {} is not available for booking {}", driver.getDriverId(),
                            booking.getBookingId());
                    booking.setDriverRequired(false);
                    booking.setDriverFee(0.0);
                    booking.setDriverAssignmentMessage("No drivers available.");
                    return;
                }
            } else {
                driver = driverRepository.findFirstByAvailableAndHasOwnCarFalse(true)
                        .orElse(null);
                if (driver == null) {
                    log.warn("No available driver found for booking {}", booking.getBookingId());
                    booking.setDriverRequired(false);
                    booking.setDriverFee(0.0);
                    booking.setDriverAssignmentMessage("No drivers available.");
                    return;
                }
            }

            booking.setDriverId(driver.getDriverId());
            booking.setDriverFee(DRIVER_FEE);
            booking.setDriverAssignmentMessage("Driver assigned successfully.");
            driver.setAvailable(false);
            driverRepository.save(driver);
            log.info("Assigned driver {} to booking {}", driver.getDriverId(), booking.getBookingId());
        } catch (Exception e) {
            log.error("Failed to assign driver to booking {}: {}", booking.getBookingId(), e.getMessage());
            booking.setDriverRequired(false);
            booking.setDriverFee(0.0);
            booking.setDriverAssignmentMessage("Failed to assign driver due to an error.");
        }
    }

    @Override
    @Transactional
    public Booking cancelBooking(String customerId, CancellationRequest request) {
        log.info("Cancelling booking with ID: {} for customer: {}", request.getBookingId(), customerId);

        if (request.getBookingId() == null || request.getBookingId().isEmpty()) {
            throw new IllegalArgumentException("Booking ID cannot be null or empty");
        }

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> {
                    log.error("Booking not found with ID: {}", request.getBookingId());
                    return new ResourceNotFoundException("Booking not found or already deleted");
                });

        if (!booking.getCustomerId().equals(customerId)) {
            log.warn("Unauthorized cancellation attempt for booking: {} by customer: {}", request.getBookingId(),
                    customerId);
            throw new UnauthorizedException("Not authorized to cancel this booking");
        }

        if (!booking.canBeCancelled()) {
            log.warn("Invalid cancellation attempt for booking: {} in state: {}", request.getBookingId(),
                    booking.getStatus());
            throw new InvalidBookingStateException("Booking cannot be cancelled in current state");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(request.getReason());
        booking.setCancellationTime(LocalDateTime.now().format(DATE_FORMATTER));

        releaseBookingResource(booking);
        handleCancellationRefund(booking);

        bookingRepository.save(booking);

        // Send cancellation email
        sendBookingStatusUpdateEmail(booking, "Booking Cancelled");

        log.info("Successfully cancelled booking with ID: {} for customer: {}", booking.getBookingId(),
                booking.getCustomerId());
        return booking;
    }

    private void releaseBookingResource(Booking booking) {
        if (booking.getCarId() != null) {
            Car car = carRepository.findById(booking.getCarId()).orElse(null);
            if (car != null && !car.isAvailable()) {
                car.setAvailable(true);
                carRepository.save(car);
                log.info("Released car {} from booking {}", car.getCarId(), booking.getBookingId());
            }
        }

        if (booking.getDriverId() != null) {
            Driver driver = driverRepository.findById(booking.getDriverId()).orElse(null);
            if (driver != null && !driver.isAvailable()) {
                driver.setAvailable(true);
                driverRepository.save(driver);
                log.info("Released driver {} from booking {}", driver.getDriverId(), booking.getBookingId());
            }
        }
    }

    private void handleCancellationRefund(Booking booking) {
        LocalDateTime pickupDateTime = parsePickupDate(booking.getPickupDate());
        LocalDateTime cancellationDeadline = pickupDateTime.minusHours(CANCELLATION_WINDOW_HOURS);
        if (LocalDateTime.now().isBefore(cancellationDeadline)) {
            booking.setRefundAmount(booking.getTotalAmount());
        } else {
            double cancellationFee = booking.getTotalAmount() * CANCELLATION_FEE_PERCENTAGE;
            booking.setRefundAmount(booking.getTotalAmount() - cancellationFee);
        }
        log.info("Processing refund of {} for booking {}", booking.getRefundAmount(), booking.getBookingId());
    }

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void checkAndUpdateCarAvailability() {
        ZoneId sriLankaZoneId = ZoneId.of("Asia/Colombo");
        LocalDateTime now = LocalDateTime.now(sriLankaZoneId);

        // Format current date and time as strings
        String currentDateStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String currentTimeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        // Get bookings with passed pickup time using the new repository method
        List<Booking> passedBookings = bookingRepository.findBookingsWithPassedPickupTime(
                List.of(BookingStatus.PENDING.toString(), BookingStatus.CONFIRMED.toString()),
                currentDateStr,
                currentTimeStr);

        for (Booking booking : passedBookings) {
            String carId = booking.getCarId();
            Optional<Car> car = carRepository.findById(carId);
            car.ifPresent(c -> {
                c.setAvailable(false);
                carRepository.save(c);
                log.info("Car {} marked as unavailable due to past pickup time for booking {}",
                        carId, booking.getBookingId());
            });

            updateBookingStatus(booking);
        }

        log.info("Completed periodic booking status check at {}", now);
    }

    private void updateBookingStatus(Booking booking) {
        if (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.CONFIRMED) {
            booking.setStatus(BookingStatus.IN_PROGRESS);
            bookingRepository.save(booking);
            log.info("Updated booking {} status to IN_PROGRESS", booking.getBookingId());
        }
    }

    private LocalDateTime parsePickupDate(String pickupDate) {
        try {
            return LocalDateTime.parse(pickupDate, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                LocalDate date = LocalDate.parse(pickupDate, DATE_ONLY_FORMATTER);
                return date.atStartOfDay();
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Invalid date format: " + pickupDate, e2);
            }
        }
    }

    private void sendBookingConfirmationEmail(Booking booking) {
        try {
            Customer customer = customerRepository.findById(booking.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

            Car car = carRepository.findById(booking.getCarId())
                    .orElseThrow(() -> new ResourceNotFoundException("Car not found"));

            String subject = "MegaCityCab - Booking Confirmation #" + booking.getBookingId();
            String emailBody = generateBookingEmailBody(booking, customer, car);

            emailService.sendHtmlEmail(customer.getEmail(), subject, emailBody);
            log.info("Booking confirmation email sent to customer: {}", customer.getEmail());
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email: {}", e.getMessage(), e);
        }
    }

    private void sendBookingStatusUpdateEmail(Booking booking, String statusMessage) {
        try {
            Customer customer = customerRepository.findById(booking.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

            String subject = "MegaCityCab - " + statusMessage + " #" + booking.getBookingId();
            StringBuilder emailBody = new StringBuilder();

            emailBody.append("Dear ").append(customer.getCustomerName()).append(",\n\n");
            emailBody.append("Your booking with ID: ").append(booking.getBookingId()).append(" has been ")
                    .append(statusMessage.toLowerCase()).append(".\n\n");

            if (booking.getStatus() == BookingStatus.CANCELLED && booking.getRefundAmount() > 0) {
                emailBody.append("A refund of $").append(booking.getRefundAmount())
                        .append(" will be processed to your original payment method.\n\n");
            }

            emailBody.append("If you have any questions, please contact our customer service team.\n");
            emailBody.append("Phone: +94 11 123 4567\n");
            emailBody.append("Email: support@megacitycab.com\n\n");

            emailBody.append("Thank you for choosing MegaCityCab!\n");

            emailService.sendHtmlEmail(customer.getEmail(), subject, emailBody.toString());
            log.info("Booking status update email sent to customer: {}", customer.getEmail());
        } catch (Exception e) {
            log.error("Failed to send booking status update email: {}", e.getMessage(), e);
        }
    }

    private String generateBookingEmailBody(Booking booking, Customer customer, Car car) {
        StringBuilder emailBody = new StringBuilder();

        // Define the SVG logo as a string
        String modernCarLogoSvg = "<svg viewBox=\"0 0 24 24\" width=\"180\" height=\"180\" fill=\"none\" stroke=\"#0c2346\" stroke-width=\"1.5\">"
                +
                "<path d=\"M3,12 C3,12 3,14 3,15\" />" +
                "<path d=\"M21,12 C21,12 21,14 21,15\" />" +
                "<path d=\"M4,10 C7,9 17,9 20,10 L19,14 C16,13 8,13 5,14 L4,10z\" stroke-linejoin=\"round\" />" +
                "<path d=\"M7,10 C9,7.5 15,7.5 17,10\" stroke-linecap=\"round\" />" +
                "<circle cx=\"7.5\" cy=\"14\" r=\"2\" />" +
                "<circle cx=\"16.5\" cy=\"14\" r=\"2\" />" +
                "<path d=\"M19,11.5 L18.5,13\" stroke-linecap=\"round\" />" +
                "<path d=\"M5,11.5 L5.5,13\" stroke-linecap=\"round\" />" +
                "<path d=\"M9.5,10.5 L14.5,10.5\" stroke-linecap=\"round\" stroke-width=\"1\" />" +
                "</svg>";

        emailBody.append("<html>")
                .append("<head>")
                .append("<style>")
                .append("@import url('https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600;700&display=swap');")
                .append("body { font-family: 'Poppins', Arial, sans-serif; background-color: #f0f4f8; margin: 0; padding: 0; color: #333; }")
                .append(".container { width: 100%; padding: 30px 0; }")
                .append(".content { max-width: 650px; margin: auto; background-color: #ffffff; padding: 0; border-radius: 12px; box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1); overflow: hidden; }")
                .append(".header { text-align: center; padding: 30px 20px; background-color: #0c2346; color: white; }")
                .append(".logo-container { display: inline-block; background-color: white; padding: 15px; border-radius: 12px; margin-bottom: 15px; }")
                .append(".header h1 { margin: 10px 0 0; font-weight: 600; font-size: 26px; }")
                .append(".header p { margin: 5px 0 0; font-size: 14px; opacity: 0.9; }")
                .append(".golden-bar { height: 8px; background-color: #eab308; }")
                .append(".main-content { padding: 30px; }")
                .append(".greeting { font-size: 18px; margin-bottom: 25px; }")
                .append(".details { margin: 25px 0; background-color: #f8fafc; border-radius: 12px; overflow: hidden; }")
                .append(".details h2 { background-color: #0c2346; color: #ffffff; padding: 15px; margin: 0; font-size: 18px; display: flex; align-items: center; }")
                .append(".details h2 svg { margin-right: 10px; }")
                .append(".details-content { padding: 20px; }")
                .append(".details-row { display: flex; margin-bottom: 12px; align-items: flex-start; }")
                .append(".details-label { font-weight: 500; width: 40%; color: #64748b; }")
                .append(".details-value { width: 60%; color: #0f172a; }")
                .append(".invoice { background-color: #f8fafc; padding: 0; border-radius: 12px; overflow: hidden; margin: 25px 0; }")
                .append(".invoice table { width: 100%; border-collapse: collapse; }")
                .append(".invoice th, .invoice td { padding: 15px; text-align: left; }")
                .append(".invoice th { background-color: #0c2346; color: #ffffff; font-weight: 500; }")
                .append(".invoice tr:not(:last-child) td { border-bottom: 1px solid #e2e8f0; }")
                .append(".invoice td.amount { text-align: right; }")
                .append(".total-row { background-color: #f1f5f9; }")
                .append(".total { font-weight: 600; color: #0c2346; }")
                .append(".policy { background-color: #fffbeb; border-left: 4px solid #eab308; padding: 15px; margin: 25px 0; border-radius: 8px; }")
                .append(".policy h3 { margin: 0 0 10px; color: #0c2346; font-size: 16px; }")
                .append(".policy p { margin: 8px 0; font-size: 14px; color: #334155; }")
                .append(".contact { background-color: #f8fafc; padding: 20px; border-radius: 12px; margin: 25px 0; }")
                .append(".contact h3 { margin: 0 0 15px; color: #0c2346; font-size: 16px; }")
                .append(".contact-info { display: flex; margin: 8px 0; }")
                .append(".contact-info svg { margin-right: 10px; min-width: 20px; color: #0c2346; }")
                .append(".thank-you { text-align: center; margin: 30px 0; font-size: 18px; color: #0c2346; font-weight: 500; }")
                .append(".footer { background-color: #0c2346; color: white; text-align: center; padding: 20px; font-size: 14px; }")
                .append(".footer p { margin: 5px 0; opacity: 0.8; }")
                .append(".social-links { margin-top: 15px; }")
                .append(".social-links a { display: inline-block; margin: 0 10px; color: white; text-decoration: none; }")
                .append("</style>")
                .append("</head>")
                .append("<body>")
                .append("<div class='container'>")
                .append("<div class='content'>")
                .append("<div class='header'>")
                .append("<div class='logo-container'>")
                .append(modernCarLogoSvg) // Replaced img tag with SVG
                .append("</div>")
                .append("<h1>Booking Confirmed</h1>")
                .append("<p>Your ride is ready to go!</p>")
                .append("</div>")
                .append("<div class='golden-bar'></div>")
                .append("<div class='main-content'>")
                .append("<div class='greeting'>Dear <strong>").append(customer.getCustomerName())
                .append("</strong>,</div>")
                .append("<p>Thank you for choosing MegaCityCab for your journey. We're excited to serve you with the best transportation experience in the city. Your booking has been confirmed with the following details:</p>")

                .append("<div class='details'>")
                .append("<h2>")
                .append("<svg xmlns='http://www.w3.org/2000/svg' width='20' height='20' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z'></path><polyline points='14 2 14 8 20 8'></polyline><line x1='16' y1='13' x2='8' y2='13'></line><line x1='16' y1='17' x2='8' y2='17'></line><polyline points='10 9 9 9 8 9'></polyline></svg>")
                .append("Booking Details")
                .append("</h2>")
                .append("<div class='details-content'>")
                .append("<div class='details-row'><div class='details-label'>Booking ID:</div><div class='details-value'>")
                .append(booking.getBookingId()).append("</div></div>")
                .append("<div class='details-row'><div class='details-label'>Booking Date:</div><div class='details-value'>")
                .append(booking.getBookingDate()).append("</div></div>")
                .append("<div class='details-row'><div class='details-label'>Pickup Location:</div><div class='details-value'>")
                .append(booking.getPickupLocation()).append("</div></div>")
                .append("<div class='details-row'><div class='details-label'>Destination:</div><div class='details-value'>")
                .append(booking.getDestination()).append("</div></div>")
                .append("<div class='details-row'><div class='details-label'>Pickup Date:</div><div class='details-value'>")
                .append(booking.getPickupDate()).append("</div></div>")
                .append("<div class='details-row'><div class='details-label'>Pickup Time:</div><div class='details-value'>")
                .append(booking.getPickupTime()).append("</div></div>")
                .append("</div>")
                .append("</div>")

                .append("<div class='details'>")
                .append("<h2>")
                .append("<svg xmlns='http://www.w3.org/2000/svg' width='20' height='20' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><rect x='1' y='3' width='15' height='13'></rect><polygon points='16 8 20 8 23 11 23 16 16 16 16 8'></polygon><circle cx='5.5' cy='18.5' r='2.5'></circle><circle cx='18.5' cy='18.5' r='2.5'></circle></svg>")
                .append("Vehicle Details")
                .append("</h2>")
                .append("<div class='details-content'>")
                .append("<div class='details-row'><div class='details-label'>Car Model:</div><div class='details-value'>")
                .append(car.getCarModel()).append("</div></div>")
                .append("<div class='details-row'><div class='details-label'>License Plate:</div><div class='details-value'>")
                .append(car.getCarLicensePlate()).append("</div></div>")
                .append("<div class='details-row'><div class='details-label'>Driver Requested:</div><div class='details-value'>")
                .append(booking.isDriverRequired() ? "Yes" : "No").append("</div></div>")
                .append("</div>")
                .append("</div>")

                .append("<div class='invoice'>")
                .append("<h2>")
                .append("<svg xmlns='http://www.w3.org/2000/svg' width='20' height='20' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><rect x='1' y='4' width='22' height='16' rx='2' ry='2'></rect><line x1='1' y1='10' x2='23' y2='10'></line></svg>")
                .append("Payment Details")
                .append("</h2>")
                .append("<table>")
                .append("<tr><th>Description</th><th>Amount</th></tr>")
                .append("<tr><td>Base Rate</td><td class='amount'>Rs. ").append(car.getBaseRate()).append("</td></tr>")
                .append("<tr><td>Distance Fare (").append(booking.getDistance())
                .append(" km)</td><td class='amount'>Rs. ").append(booking.getDistanceFare()).append("</td></tr>")
                .append("<tr><td>Tax</td><td class='amount'>Rs. ").append(booking.getTax()).append("</td></tr>");
        if (booking.isDriverRequired() && booking.getDriverFee() > 0) {
            emailBody.append("<tr><td>Driver Fee</td><td class='amount'>Rs. ").append(booking.getDriverFee())
                    .append("</td></tr>");
        }
        emailBody.append("<tr class='total-row'><td class='total'>Total Amount</td><td class='amount total'>Rs. ")
                .append(booking.getTotalAmount()).append("</td></tr>")
                .append("</table>")
                .append("</div>")

                .append("<div class='policy'>")
                .append("<h3>Cancellation Policy</h3>")
                .append("<p>• Cancellations made more than 24 hours before the pickup time will receive a full refund.</p>")
                .append("<p>• Cancellations made within 24 hours of the pickup time will incur a 10% cancellation fee.</p>")
                .append("</div>")

                .append("<div class='contact'>")
                .append("<h3>Need Help?</h3>")
                .append("<p>If you have any questions or need to make changes to your booking, our customer service team is ready to assist you:</p>")
                .append("<div class='contact-info'>")
                .append("<svg xmlns='http://www.w3.org/2000/svg' width='20' height='20' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z'></path></svg>")
                .append("<span>+94 11 123 4567</span>")
                .append("</div>")
                .append("<div class='contact-info'>")
                .append("<svg xmlns='http://www.w3.org/2000/svg' width='20' height='20' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z'></path><polyline points='22,6 12,13 2,6'></polyline></svg>")
                .append("<span>support@megacitycab.com</span>")
                .append("</div>")
                .append("</div>")

                .append("<div class='thank-you'>Thank you for choosing MegaCityCab!</div>")
                .append("</div>") // end main-content

                .append("<div class='footer'>")
                .append("<p>© 2025 MegaCityCab. All rights reserved.</p>")
                .append("<div class='social-links'>")
                .append("<a href='#'><svg xmlns='http://www.w3.org/2000/svg' width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z'></path></svg></a>")
                .append("<a href='#'><svg xmlns='http://www.w3.org/2000/svg' width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><rect x='2' y='2' width='20' height='20' rx='5' ry='5'></rect><path d='M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z'></path><line x1='17.5' y1='6.5' x2='17.51' y2='6.5'></line></svg></a>")
                .append("<a href='#'><svg xmlns='http://www.w3.org/2000/svg' width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M23 3a10.9 10.9 0 0 1-3.14 1.53 4.48 4.48 0 0 0-7.86 3v1A10.66 10.66 0 0 1 3 4s-4 9 5 13a11.64 11.64 0 0 1-7 2c9 5 20 0 20-11.5a4.5 4.5 0 0 0-.08-.83A7.72 7.72 0 0 0 23 3z'></path></svg></a>")
                .append("</div>")
                .append("</div>")

                .append("</div>")
                .append("</div>")
                .append("</body>")
                .append("</html>");

        return emailBody.toString();
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
    public List<Booking> getAvailableBookings() {
        // Return bookings that are PENDING and have no assigned driver
        return bookingRepository.findByDriverIdIsNullAndStatus(BookingStatus.PENDING);
    }
}