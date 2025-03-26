package com.example.demo.validator;

import com.example.demo.dto.userdto.UserDTO;

import java.util.ArrayList;
import java.util.List;

public class UserFieldValidator {

    public static List<String> validateInsertOrUpdate(UserDTO userDTO) {
        List<String> errors = new ArrayList<>();

        if (userDTO == null) {
            errors.add("Request data is missing");
        } else {
            // Name validation
            if (userDTO.getName() == null || userDTO.getName().trim().isEmpty()) {
                errors.add("Name cannot be empty");
            }

            // Email validation
            if (userDTO.getEmail() == null || !userDTO.getEmail().matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                errors.add("Invalid email format");
            }

            // Password validation
            if (userDTO.getPassword() == null || userDTO.getPassword().length() < 6) {
                errors.add("Password must be at least 6 characters");
            }

            // Role validation
            if (userDTO.getRoleName() == null || !userDTO.getRoleName().matches("^[A-Z_]+$")) {
                errors.add("Role name must be uppercase (e.g., 'USER', 'ADMIN')");
            }
        }
        return errors;
    }
}
