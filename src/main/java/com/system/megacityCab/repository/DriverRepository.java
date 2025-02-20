package com.system.megacityCab.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.system.megacityCab.model.Driver;

public interface DriverRepository extends MongoRepository<Driver,String>{

    Optional<Driver> findByDriverEmail(String driverEmail);
    boolean existsByEmail(String email);
    List<Driver> findByAvailable(boolean available);
    Optional<Driver> findFirstByAvailableAndHasOwnCarFalse(boolean available);
    Optional<Driver> findByCarId(String carId);
    
    
}
