package com.system.megacityCab.service;

import java.util.List;

import org.springframework.stereotype.Service;


import com.system.megacityCab.model.Admin;



@Service
public interface AdminService {

    List<Admin> getAllAdmins();
    Admin getAdminById(String adminId);
    Admin createAdmin(Admin admin);
    Admin updateAdmin(String userId, Admin admin);
   
    
}
