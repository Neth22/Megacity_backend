package com.system.megacityCab.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "admins")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Admin {

    @Id
    private String adminId;

    private String adminName;

    private String email;

    private String password;
    
    private String role ="ADMIN";
}
