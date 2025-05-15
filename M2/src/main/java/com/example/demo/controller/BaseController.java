package com.example.demo.controller;

import com.example.demo.errorhandler.UserException;
import jakarta.servlet.http.HttpServletRequest; // Import HttpServletRequest

public abstract class BaseController {

    public BaseController() {
    }

    protected Long getCurrentUserId(HttpServletRequest request) throws UserException {
        Object userIdAttribute = request.getAttribute("userId");

        if (userIdAttribute == null) {

            throw new UserException("User ID not found in request context. Check interceptor or if endpoint requires authentication.");
        }

        if (!(userIdAttribute instanceof Long)) {

            throw new UserException("Invalid User ID format in request context.");
        }

        return (Long) userIdAttribute;
    }

}