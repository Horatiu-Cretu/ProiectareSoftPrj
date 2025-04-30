package com.example.demo.controller;

import com.example.demo.errorhandler.UserException;
import jakarta.servlet.http.HttpServletRequest; // Import HttpServletRequest
// Removed unused imports: UserRepository, RestTemplate, Autowired, HttpHeaders

public abstract class BaseController {

    // No dependencies needed in the base controller itself anymore.

    // Parameterless constructor - does not need @Autowired
    public BaseController() {
        // Constructor logic (if any) can go here, but likely empty.
    }

    // Method to get user ID from the request attribute set by the interceptor
    protected Long getCurrentUserId(HttpServletRequest request) throws UserException {
        Object userIdAttribute = request.getAttribute("userId");

        if (userIdAttribute == null) {
            // This indicates the interceptor didn't run or failed to set the attribute,
            // or the endpoint was public and the attribute wasn't set. Handle appropriately.
            // Consider logging this situation.
            throw new UserException("User ID not found in request context. Check interceptor or if endpoint requires authentication.");
        }

        if (!(userIdAttribute instanceof Long)) {
            // Should not happen if interceptor sets it correctly, but good practice to check
            // Consider logging this type mismatch.
            throw new UserException("Invalid User ID format in request context.");
        }

        return (Long) userIdAttribute;
    }

    // --- Keep other helper methods if needed ---
}