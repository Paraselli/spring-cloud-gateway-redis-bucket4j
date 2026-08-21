package com.example.customer;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    @GetMapping("/{id}")
    public Map<String, Object> getCustomer(@PathVariable Long id) {
        return Map.of(
                "id", id,
                "name", "Ram",
                "message", "Response from Customer Service"
        );
    }
}
