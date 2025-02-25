package com.system.megacityCab.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.system.megacityCab.model.Booking;
import com.system.megacityCab.model.Car;
import com.system.megacityCab.model.Driver;

@Service

public interface DriverService {

    List<Driver> getAllDrivers();
    Driver getDriverById(String driverId);
    ResponseEntity<?> createDriver(Driver driver , Car car);
    Driver updateDriver(String driverId, Driver driver);
    void deleteDriver(String driverId);
    Driver updateAvailability(String driverId, boolean availability);
    List<Booking> getDriverBookings(String driverId);
    
}
