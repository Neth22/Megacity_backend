package com.system.megacityCab.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.system.megacityCab.dto.LoginDTO;
import com.system.megacityCab.service.UserDetailsServiceImpl;
import com.system.megacityCab.util.JwtUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login (@RequestBody LoginDTO loginDTO) throws Exception {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword()));
        UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(loginDTO.getEmail());
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        String token = jwtUtil.generateToken(userDetails,role);
        return ResponseEntity.ok(new AuthResponse(token,role));

    }

    
}

class AuthResponse{

    private String token;
    private String  role;

    public AuthResponse (String token, String role){

        this.token = token;
        this.role = role;
    }


    public String getToken(){return token;}
    public String getRole(){return role;}
}