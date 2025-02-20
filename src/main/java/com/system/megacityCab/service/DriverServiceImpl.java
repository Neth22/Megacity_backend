package com.system.megacityCab.service;

import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.system.megacityCab.model.Driver;

import com.system.megacityCab.repository.DriverRepository;

@Service

public class DriverServiceImpl implements DriverService{

    @Autowired
    private DriverRepository driverRepository;

    @Override
    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    @Override
    public Driver getDriverById(String driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + driverId));
    }

    @Override
    public Driver createDriver(Driver driver) {

        return driverRepository.save(driver);
    }

    @Override
    public Driver updateDriver(String driverId, Driver driver) {
       
        Driver existingDriver = getDriverById(driverId);

        existingDriver.setDriverName(driver.getDriverName());
        existingDriver.setDriverEmail(driver.getDriverEmail());
        existingDriver.setDriverLicenseNo(driver.getDriverLicenseNo());
        existingDriver.setDriverPhoneNum(driver.getDriverPhoneNum());
        existingDriver.setPassword(driver.getPassword());


        return driverRepository.save(existingDriver);
    }

    @Override
    public void deleteDriver(String driverId) {

        driverRepository.deleteById(driverId);
        
    }

    
}
