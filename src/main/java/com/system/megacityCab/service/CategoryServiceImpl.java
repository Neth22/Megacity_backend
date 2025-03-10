package com.system.megacityCab.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.system.megacityCab.model.Category;
import com.system.megacityCab.repository.CategoryRepository;

@Service

public class CategoryServiceImpl implements CategoryService{
    
      @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(String categoryId, Category category) {
        return categoryRepository.findById(categoryId)
                .map(existCategory ->{
                    existCategory.setCategoryName(category.getCategoryName());
                    existCategory.setPricePerKm(category.getPricePerKm());
                    return categoryRepository.save(existCategory);
                })
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    @Override
    public void deleteCategory(String categoryId) {
        categoryRepository.deleteById(categoryId);
    }
}
