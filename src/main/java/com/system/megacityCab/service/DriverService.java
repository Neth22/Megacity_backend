package com.system.megacityCab.service;

import java.util.List;

import org.springframework.stereotype.Service;


import com.system.megacityCab.model.Driver;

@Service

public interface DriverService {

    List<Driver> getAllDrivers();
    Driver getDriverById(String driverId);
    Driver createDriver(Driver driver);
    Driver updateDriver(String driverId, Driver driver);
    void deleteDriver(String driverId);
    
}
