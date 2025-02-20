package com.system.megacityCab.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.system.megacityCab.model.Driver;
import com.system.megacityCab.service.DriverService;

@RestController
@RequestMapping("/auth/drivers")
@CrossOrigin(origins = "*")

public class DriverController {

    @Autowired
    private DriverService driverService;

    @GetMapping
    public ResponseEntity<List<Driver>> getAllDrivers() {
        List<Driver> drivers = driverService.getAllDrivers();
        return new ResponseEntity<>(drivers, HttpStatus.OK);
    }

    @GetMapping("/viewById/{driverId}")
    public ResponseEntity<Driver> getDriverById(@PathVariable String driverId) {
        try {
            Driver driver = driverService.getDriverById(driverId);
            return new ResponseEntity<>(driver, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/createDriver")
    public ResponseEntity<Driver> createDriver(@RequestBody Driver driver) {
        try {
            Driver createdDriver = driverService.createDriver(driver);
            return new ResponseEntity<>(createdDriver, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/updateDriver/{driverId}")
    public ResponseEntity<Driver> updateDriver(
            @PathVariable String driverId,
            @RequestBody Driver driver) {
        try {
            Driver updatedDriver = driverService.updateDriver(driverId, driver);
            return new ResponseEntity<>(updatedDriver, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/deleteDriver/{driverId}")
    public ResponseEntity<Void> deleteDriver(@PathVariable String driverId) {
        try {
            driverService.deleteDriver(driverId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    
}
