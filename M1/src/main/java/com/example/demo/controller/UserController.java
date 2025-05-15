package com.example.demo.controller;

import com.example.demo.builder.userbuilder.UserBuilder;
import com.example.demo.dto.authdto.AuthResponse;
import com.example.demo.dto.userdto.UserDTO;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.errorhandler.UserException;
import com.example.demo.repository.RoleRepository;
import com.example.demo.service.JWTService;
import com.example.demo.service.UserService;
import com.example.demo.validator.UserFieldValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(value = "/api/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final JWTService jwtService;
    private final RoleRepository roleRepository;

    @Autowired
    public UserController(UserService userService, JWTService jwtService, RoleRepository roleRepository) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.roleRepository = roleRepository;
    }

    @Transactional
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserDTO userDTO) throws UserException {
        List<String> errors = UserFieldValidator.validateInsertOrUpdate(userDTO);
        if (!errors.isEmpty()) {
            throw new UserException(String.join("; ", errors));
        }

        Role assignedRole;
        String requestedRoleName = userDTO.getRoleName();

        if (requestedRoleName != null && !requestedRoleName.trim().isEmpty()) {
            String roleNameToFind = requestedRoleName.trim().toUpperCase();
            assignedRole = roleRepository.findByName(roleNameToFind);

            if (assignedRole == null) {
                if (roleNameToFind.equals("ADMIN")) {
                    logger.error("CRITICAL: ADMIN role specified during registration for {} but not found in database.", userDTO.getEmail());
                    throw new UserException("Configuration error: ADMIN role not found. Cannot register admin.");
                } else {
                    logger.warn("Role '{}' requested by {} not found. Defaulting to USER role.", roleNameToFind, userDTO.getEmail());
                    assignedRole = roleRepository.findByName("USER");
                    if (assignedRole == null) {
                        logger.info("USER role not found, creating it.");
                        assignedRole = roleRepository.save(new Role("USER"));
                    }
                }
            }
        } else {
            assignedRole = roleRepository.findByName("USER");
            if (assignedRole == null) {
                logger.info("USER role not found, creating it.");
                assignedRole = roleRepository.save(new Role("USER"));
            }
        }

        User user = UserBuilder.generateEntityFromDTO(userDTO, assignedRole);
        userService.register(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User registered successfully with role: " + assignedRole.getName());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody User user) throws UserException {
        AuthResponse response = userService.login(user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/getAll")
    public ResponseEntity<?> displayAllUserView() {
        return new ResponseEntity<>(userService.findAllUserView(), HttpStatus.OK);
    }

    @GetMapping("/getUserById/{id}")
    public ResponseEntity<?> displayUserViewById(@PathVariable("id") @NonNull Long id) throws UserException {
        return new ResponseEntity<>(userService.findUserViewById(id), HttpStatus.OK);
    }

    @GetMapping("/getUserByEmail/{email}")
    public ResponseEntity<?> displayUserViewByEmail(@PathVariable("email") String email) throws UserException {
        return new ResponseEntity<>(userService.findUserViewByEmail(email), HttpStatus.OK);
    }

    @GetMapping("/getUserByRoleName/{roleName}")
    public ResponseEntity<?> displayUserViewByRoleName(@PathVariable("roleName") String roleName) throws UserException {
        return new ResponseEntity<>(userService.findUserViewByRoleName(roleName), HttpStatus.OK);
    }

    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> processAddUserForm(@RequestBody UserDTO userDTO) throws UserException {
        return new ResponseEntity<>(userService.createUser(userDTO), HttpStatus.CREATED);
    }

    @PutMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> processUpdateUserForm(@RequestBody UserDTO userDTO) throws UserException {
        return new ResponseEntity<>(userService.updateUser(userDTO), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUserByIdForm(@PathVariable("id") Long id) throws UserException {
        userService.deleteUser(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}