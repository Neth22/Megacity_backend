package com.system.megacityCab.controller;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.system.megacityCab.exception.ResourceNotFoundException;
import com.system.megacityCab.model.Booking;
import com.system.megacityCab.model.Car;
import com.system.megacityCab.model.Customer;
import com.system.megacityCab.model.Driver;
import com.system.megacityCab.repository.CustomerRepository;
import com.system.megacityCab.service.BookingService;
import com.system.megacityCab.service.CloudinaryService;
import com.system.megacityCab.service.DriverService;

import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/auth/driver")
@Slf4j
public class DriverController {

    @Autowired
    private DriverService driverService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BookingService bookingService;

    @GetMapping("/getalldrivers")
    public List<Driver> getAllDrivers() {
        return driverService.getAllDrivers();
    }

    @GetMapping("/getdriver/{driverId}")
public ResponseEntity<Driver> getDriverById(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable String driverId) {
    String email = userDetails.getUsername();
    Driver driver = driverService.getDriverById(driverId);

    // Allow the driver to view their own profile or if they have a booking
    boolean isOwnProfile = driver.getEmail().equals(email);
    boolean hasBooking = bookingService.hasBookingWithDriver(email, driverId);

    if (!isOwnProfile && !hasBooking) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    return ResponseEntity.ok(driver);
}

    @PostMapping(value = "/createdriver",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createDriver(
            @RequestParam("driverName") String driverName,
            @RequestParam("email") String email,
            @RequestParam("driverLicenseNo") String driverLicenseNo,
            @RequestParam("driverPhoneNum") String driverPhoneNum,
            @RequestParam("password") String password,
            @RequestParam("hasOwnCar") boolean hasOwnCar,
            @RequestParam(value = "carLicensePlate", required = false) String carLicensePlate,
            @RequestParam(value = "carBrand", required = false) String carBrand,
            @RequestParam(value = "carModel", required = false) String carModel,
            @RequestParam(value = "capacity", required = false) Integer capacity,
            @RequestParam(value = "baseRate", required = false) Double baseRate,
            @RequestParam(value = "driverRate", required = false) Double driverRate,
            @RequestParam(value = "carImg", required = false) MultipartFile carImg
     ) {

        try {
            Driver driver = new Driver();
            driver.setDriverName(driverName);
            driver.setEmail(email);
            driver.setDriverLicenseNo(driverLicenseNo);
            driver.setDriverPhoneNum(driverPhoneNum);
            driver.setPassword(password);
            driver.setHasOwnCar(hasOwnCar);

          
            Car car = null;
            if (hasOwnCar) {
                car = new Car();
                car.setCarLicensePlate(carLicensePlate);
                car.setCarBrand(carBrand);
                car.setCarModel(carModel);
                car.setCapacity(capacity != null ? capacity : 4);
                car.setBaseRate(baseRate != null ? baseRate : 0.0);
                car.setDriverRate(driverRate != null ? driverRate : 0.0);
                car.setAssignedDriverId(email);
                if (carImg != null && !carImg.isEmpty()) {
                    String carImageUrl = cloudinaryService.uploadImage(carImg);
                    car.setCarImgUrl(carImageUrl);
                }
            }

            return driverService.createDriver(driver, car);

        } catch (Exception e) {
            log.error("Error creating driver: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating driver: " + e.getMessage());
        }
    }

    @PutMapping("/updatedriver/{driverId}")
    public ResponseEntity<Driver> updateDriver(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String driverId,
            @RequestBody Driver driver) {
        String email = userDetails.getUsername();
        log.info("Updating driver with ID: {} for email: {}", driverId, email);

        Driver updatedDriver = driverService.updateDriver(driverId, driver);
        return ResponseEntity.ok(updatedDriver);
    }

    @PutMapping("/{driverId}/availability")
    public ResponseEntity<Driver> updateAvailability(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String driverId,
            @RequestBody Map<String, Boolean> availability) {
        String email = userDetails.getUsername();
        log.info("Updating availability for driver: {} for email: {}", driverId, email);

        if (!availability.containsKey("availability")) {
            return ResponseEntity.badRequest().build();
        }

        Driver driver = driverService.updateAvailability(driverId, availability.get("availability"));
        return ResponseEntity.ok(driver);
    }

    @GetMapping("/{driverId}/bookings")
    public ResponseEntity<List<Booking>> getDriverBookings(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String driverId) {
        String email = userDetails.getUsername();
        log.info("Fetching bookings for driver: {} for email: {}", driverId, email);

        Driver driver = driverService.getDriverById(driverId);
        if (!email.equals(driver.getEmail())) {
            log.warn("Unauthorized access attempt by user: {}", email);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Booking> bookings = driverService.getDriverBookings(driverId).stream()
                .map(booking -> {
                    Customer customer = customerRepository.findById(booking.getCustomerId())
                            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
                    booking.setPassengerName(customer.getCustomerName());
                    
                    return booking;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(bookings);
    }

    @DeleteMapping("/{driverId}")
    public ResponseEntity<Void> deleteDriver(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String driverId) {
        String email = userDetails.getUsername();
        log.info("Deleting driver with ID: {} for email: {}", driverId, email);

        driverService.deleteDriver(driverId);
        return ResponseEntity.noContent().build();
    }

}
