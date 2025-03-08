package com.system.megacityCab.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.system.megacityCab.dto.LoginDTO;
import com.system.megacityCab.util.JwtUtil;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;


    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) throws Exception {
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword())
        );

        String token = jwtUtil.generateToken(authentication);
        String role = jwtUtil.extractRole(token);
        String userId = jwtUtil.extractUserId(token);

        return ResponseEntity.ok(new AuthResponse(token, role, userId));
    }

}

class AuthResponse{
    private String token;
    private String role;
    private String userId;

    public AuthResponse(String token, String role, String userId){
        this.token = token;
        this.role = role;
        this.userId = userId;
    }

    public String getToken(){
        return token;
    }
    public String getRole(){
        return role;
    }
    public String getUserId(){ return userId;}
}