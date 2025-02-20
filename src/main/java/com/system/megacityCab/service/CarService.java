package com.system.megacityCab.service;

import java.util.List;

import org.springframework.stereotype.Service;


import com.system.megacityCab.model.Car;

@Service

public interface CarService {

    List<Car> getAllcars();
    Car getCarById(String carId);
    Car createCar(Car car);
    Car updateCar(String carId, Car car);
    void deleteCar(String carId);
    
}
