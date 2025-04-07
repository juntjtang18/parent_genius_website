package ca.parentgeniusai.website.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.parentgeniusai.website.model.User;
import ca.parentgeniusai.website.repository.UserRepository;

@RestController
public class TestController {
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/test-user")
    public String testUser() {
        return userRepository.findByUsername("testuser").map(User::getUsername).orElse("Not found");
    }
}