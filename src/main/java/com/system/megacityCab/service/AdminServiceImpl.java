package com.system.megacityCab.service;

import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



import com.system.megacityCab.model.Admin;
import com.system.megacityCab.repository.AdminRepository;

@Service
public class AdminServiceImpl implements AdminService{

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    @Override
    public Admin getAdminById(String adminId) {
        return adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found with ID: " + adminId));
    }

    @Override
    public Admin createAdmin(Admin admin) {
         return adminRepository.save(admin);
    }

    @Override
    public Admin updateAdmin(String adminId, Admin admin) {
        
        Admin existingUser = getAdminById(adminId);

        existingUser.setAdminName(admin.getAdminName());
        existingUser.setEmail(admin.getEmail());
        existingUser.setPassword(admin.getPassword());
        

        return adminRepository.save(existingUser);
    }

   


    

    
    
}
