package com.system.megacityCab.util;

import java.util.Date;
import java.util.function.Function;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtUtil {
    private String SECRET_KEY = "kmgtkowtiork4095645kgmldfkgldkgldmgsldgkm";

    public String generateToken( UserDetails userDetails, String role){

        return Jwts.builder()
                    .setSubject(userDetails.getUsername())
                    .claim(role, role.replace("ROLE",""))
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis()+ 1000 * 60 *10))
                    .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                    .compact();
    }

    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token){
        return  extractClaim(token, claims->claims.get("role",String.class));
    }

    public<T> T extractClaim(String token , Function<Claims, T>claimsResolcer){

        Claims claims = Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token).getBody();
            return claimsResolcer.apply(claims);
        
    }

    public boolean validateToken(String token, UserDetails userDetails){

        final String username = extractUsername(token);

            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        
    }

    private boolean isTokenExpired(String token){
            return extractClaim(token,Claims::getExpiration).before(new Date());
        
    }

    
}
