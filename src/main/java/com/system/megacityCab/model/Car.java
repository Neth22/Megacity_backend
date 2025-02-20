package com.system.megacityCab.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "cars")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Car {

    @Id
    private String carId;

    private String carBrand;
    private String carModel;
    private String carLicensePlate;
    private int capacity;
    private String assignedDriverId;
    private String carImgUrl;
    private boolean available = true;
    private double baseRate;
    private double driverRate;

    
}
