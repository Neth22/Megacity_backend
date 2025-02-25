package com.system.megacityCab.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

import com.system.megacityCab.model.Booking;
import com.system.megacityCab.model.Car;
import com.system.megacityCab.model.Driver;
import com.system.megacityCab.service.DriverService;

import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/auth/driver")
@Slf4j
public class DriverController {

    @Autowired
    private DriverService driverService;

    @GetMapping("/getalldrivers")
    public List<Driver> getAllDrivers() {
        return driverService.getAllDrivers();
    }

    @GetMapping("/drivers/{id}")
    public Driver getDriverById(@PathVariable String driverId) {
        return driverService.getDriverById(driverId);
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
            @RequestParam(value = "carImage", required = false) MultipartFile carImage
            ) {

        try{
            Driver driver = new Driver();
            driver.setDriverName(driverName);
            driver.setEmail(email);
            driver.setDriverLicenseNo(driverLicenseNo);
            driver.setDriverPhoneNum(driverPhoneNum);
            driver.setPassword(password);
            driver.setHasOwnCar(hasOwnCar);

           
            Car car = null;
            if(hasOwnCar){
                car = new Car();
                car.setCarLicensePlate(carLicensePlate);
                car.setCarModel(carModel);
                car.setCarBrand(carBrand);

                
                if (capacity != null) {
                    car.setCapacity(capacity);
                } else {
                    
                    car.setCapacity(4); 
                }

                if (baseRate != null) {
                    car.setBaseRate(baseRate);
                }

                if (driverRate != null) {
                    car.setDriverRate(driverRate);
                }

                if(carImage != null && !carImage.isEmpty()){
                    String carImgUrl = handleImageUpload(carImage, "car");
                    car.setCarImgUrl(carImgUrl);
                }
            }

            return driverService.createDriver(driver, car);

        }catch (Exception e){
            log.error("Error creating driver: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating driver: " + e.getMessage());
        }
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

        List<Booking> bookings = driverService.getDriverBookings(driverId);
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

    private String handleImageUpload(MultipartFile file, String type) throws IOException {
        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        String basePath = type.equals("driver") ? "drivers/" : "cars/";
        String uploadDir = "uploads/" + basePath;

        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        Path filePath = Paths.get(uploadDir + filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return basePath + filename;
    }
  

}
