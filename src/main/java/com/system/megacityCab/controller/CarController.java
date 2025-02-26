package com.system.megacityCab.controller;

import java.io.IOException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.system.megacityCab.model.Car;
import com.system.megacityCab.service.CarService;
import com.system.megacityCab.service.CloudinaryService;

@RestController
@CrossOrigin(origins = "*")
public class CarController {

    @Autowired
    private CarService carService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping("/all/viewCars")
    public ResponseEntity<List<Car>> getAllCars() {
        return new ResponseEntity<>(carService.getAllcars(), HttpStatus.OK);
    }

    @GetMapping("/all/getCar/{id}")
    public ResponseEntity<Car> getCarById(@PathVariable("id") String carId) {
        return new ResponseEntity<>(carService.getCarById(carId), HttpStatus.OK);
    }

    @PostMapping("/auth/cars/createCar")
    public ResponseEntity<Car> createCar(@RequestParam String carBrand,
                                         @RequestParam String carModel,
                                         @RequestParam String carLicensePlate,
                                         @RequestParam int capacity,
                                         @RequestParam MultipartFile carImg) throws IOException {
        

                String carImgUrl = cloudinaryService.uploadImage(carImg);

                Car car = new Car();
                car.setCarBrand(carBrand);
                car.setCarModel(carModel);
                car.setCarLicensePlate(carLicensePlate);
                car.setCapacity(capacity);
                car.setCarImgUrl(carImgUrl);
  
                Car savedCar = carService.createCar(car);
                return ResponseEntity.ok(savedCar);
    }

    @PutMapping("/auth/cars/updateCar/{carId}")
    public ResponseEntity<Car> updateCar(
            @PathVariable String carId,
            @RequestBody Car car) {
        Car updatedCar = carService.updateCar(carId, car);
        return new ResponseEntity<>(updatedCar, HttpStatus.OK);
    }

    @DeleteMapping("/auth/cars/{carId}")
    public ResponseEntity<Void> deleteCar(@PathVariable String carId) {
        carService.deleteCar(carId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
