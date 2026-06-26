package org.ecommerce.utility.constants;

/**
 * API response messages constants for standardized communication
 * Following RESTful API best practices
 */
public interface ApiMessages {
    
    // Registration Messages
    String REGISTRATION_SUCCESS = "User registered successfully. You can now login with your credentials.";
    String REGISTRATION_FAILED = "User registration failed. Please try again.";
    String REGISTRATION_INVALID_INPUT = "Invalid registration input. Please check all required fields.";
    String EMAIL_ALREADY_EXISTS = "Email address is already registered. Please use a different email.";
    String EMAIL_REQUIRED = "Email is required and cannot be empty.";
    String PASSWORD_REQUIRED = "Password is required and cannot be empty.";
    String INVALID_EMAIL_FORMAT = "Please provide a valid email address.";
    String PASSWORD_TOO_WEAK = "Password does not meet security requirements. Must be at least 8 characters.";
    
    // Login Messages
    String LOGIN_SUCCESS = "Login successful. Welcome back!";
    String LOGIN_FAILED = "Invalid email or password. Please try again.";
    String UNAUTHORIZED = "Unauthorized access. Please login first.";
    
    // General Messages
    String INTERNAL_ERROR = "An internal server error occurred. Please try again later.";
    String SERVICE_UNAVAILABLE = "Service is temporarily unavailable. Please try again later.";
    String INVALID_REQUEST = "Invalid request. Please check your input and try again.";
    String OPERATION_SUCCESSFUL = "Operation completed successfully.";
    
}

