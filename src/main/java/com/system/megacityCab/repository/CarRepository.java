package com.system.megacityCab.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.system.megacityCab.model.Car;
import java.util.List;


@Repository
public interface CarRepository extends MongoRepository<Car,String> {

    List<Car> findByAvailable(boolean available);
    List<Car> findByAssignedDriverId(String assignedDriverId);
    
}
