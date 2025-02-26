package com.system.megacityCab.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.system.megacityCab.model.Customer;

import com.system.megacityCab.repository.CustomerRepository;
import com.system.megacityCab.repository.DriverRepository;

@Service
public class CustomerServiceImpl implements CustomerService {

     @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    public boolean isEmailTaken(String email) {
        return customerRepository.existsByEmail(email) || 
               driverRepository.existsByEmail(email);
    }

    @Override
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Override
    public Customer getCustomerById(String customerId) {
        return customerRepository.findById(customerId).orElse(null);
    }

    @Override
    public ResponseEntity<?>  createCustomer(Customer customer) {
        if (isEmailTaken(customer.getEmail())) {
            return ResponseEntity.badRequest()
                .body("Email already exists: " + customer.getEmail());
        }
        String encodedPassword = passwordEncoder.encode(customer.getPassword());
        customer.setPassword(encodedPassword);
        return ResponseEntity.ok(customerRepository.save(customer));
    }

    @Override
    public Customer updateCustomer(String customerId, Customer customer) {
        return customerRepository.findById(customerId)
            .map(exitCustomer -> {
                exitCustomer.setCustomerName(customer.getCustomerName());
                exitCustomer.setCustomerAddress(customer.getCustomerAddress());
                exitCustomer.setCustomerPhone(customer.getCustomerPhone());
                
                return customerRepository.save(customer);
            })
            .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    @Override
    public void deleteCustomer(String customerId) {
        customerRepository.deleteById(customerId);
    }
 
    
}
