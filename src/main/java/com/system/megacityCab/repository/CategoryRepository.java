package com.system.megacityCab.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.system.megacityCab.model.Category;

@Repository
public interface CategoryRepository extends MongoRepository<Category,String>{

     
}
