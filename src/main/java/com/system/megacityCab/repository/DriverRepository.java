package com.system.megacityCab.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.system.megacityCab.model.Driver;

@Repository
public interface DriverRepository extends MongoRepository<Driver,String>{

    Optional<Driver> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Driver> findByAvailable(boolean available);
    Optional<Driver> findFirstByAvailableAndHasOwnCarFalse(boolean available);
    Optional<Driver> findByCarId(String carId);
    
    
}
