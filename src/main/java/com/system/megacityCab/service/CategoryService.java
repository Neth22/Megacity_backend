package com.system.megacityCab.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.system.megacityCab.model.Category;

@Service
public interface CategoryService {

    List<Category> getAllCategories();
    Category createCategory(Category category);
    Category updateCategory(String categoryId, Category category);
    void deleteCategory(String categoryId);
    
}
