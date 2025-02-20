package com.system.megacityCab.service;

import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.system.megacityCab.model.Car;
import com.system.megacityCab.repository.CarRepository;

@Service
public class CarServiceImpl implements CarService{

    @Autowired
    private CarRepository carRepository;

    @Override
    public List<Car> getAllcars() {
       return carRepository.findAll();
    }

    @Override
    public Car getCarById(String carId) {
        return carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + carId));
    }

    @Override
    public Car createCar(Car car) {
        return carRepository.save(car);
    }

   

    @Override
    public void deleteCar(String carId) {
        carRepository.deleteById(carId);
    }

    @Override
    public Car updateCar(String carId, Car car) {
        Car existingCar = getCarById(carId);

        existingCar.setCarBrand(car.getCarBrand());
        existingCar.setCarModel(car.getCarModel());
        existingCar.setCarLicensePlate(car.getCarLicensePlate());
        existingCar.setCapacity(car.getCapacity());
        existingCar.setCarImgUrl(car.getCarImgUrl());
        existingCar.setAvailable(car.isAvailable());
        

        return carRepository.save(existingCar);
    }

   
    
}
