package com.example.demo.service;

import com.example.demo.builder.userbuilder.UserBuilder;
import com.example.demo.builder.userbuilder.UserViewBuilder;
import com.example.demo.dto.authdto.AuthResponse;
import com.example.demo.dto.userdto.UserDTO;
import com.example.demo.dto.userdto.UserViewDTO;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.errorhandler.UserException;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.validator.UserFieldValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class); // Add logger

    @Autowired
    private final RoleRepository roleRepository;

    // It's generally better to inject the encoder bean defined in SecurityConfig
    // instead of creating a new instance here.
    // private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    private JWTService jwtService;

    public List<UserViewDTO> findAllUserView() {
        return userRepository.findAll().stream()
                .map(UserViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }

    public UserViewDTO findUserViewById(Long id) throws UserException {
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            logger.warn("User not found with id: {}", id);
            throw new UserException("User not found with id field: " + id);
        }
        return UserViewBuilder.generateDTOFromEntity(user.get());
    }

    public UserViewDTO findUserViewByEmail(String email) throws UserException {
        Optional<User> user = userRepository.findUserByEmail(email);
        if (user.isEmpty()) {
            logger.warn("User not found with email: {}", email);
            throw new UserException("User not found with email field: " + email);
        }
        return UserViewBuilder.generateDTOFromEntity(user.get());
    }


    public Long createUser(UserDTO userDTO) throws UserException {
        List<String> errors = UserFieldValidator.validateInsertOrUpdate(userDTO);
        if (!errors.isEmpty()) {
            String errorMsg = StringUtils.collectionToDelimitedString(errors, "; ");
            logger.error("User creation validation failed: {}", errorMsg);
            throw new UserException(errorMsg);
        }

        Optional<Role> role = roleRepository.findRoleByName(userDTO.getRoleName().toUpperCase());
        if (role.isEmpty()) {
            logger.error("Role not found: {}", userDTO.getRoleName().toUpperCase());
            throw new UserException("Role not found with name field: " + userDTO.getRoleName().toUpperCase());
        }

        Optional<User> user = userRepository.findUserByEmail(userDTO.getEmail());
        if (user.isPresent()) {
            logger.warn("Attempted to create user with duplicate email: {}", userDTO.getEmail());
            throw new UserException("User record does not permit duplicates for email field: " + userDTO.getEmail());
        }

        User userSave = UserBuilder.generateEntityFromDTO(userDTO, role.get());
        // Ensure password is encoded if UserBuilder doesn't handle it
        userSave.setPassword(bCryptPasswordEncoder.encode(userSave.getPassword()));

        User savedUser = userRepository.save(userSave);
        logger.info("User created successfully with ID: {}", savedUser.getId());
        return savedUser.getId();
    }

    public Long updateUser(UserDTO userDTO) throws UserException {
        List<String> errors = UserFieldValidator.validateInsertOrUpdate(userDTO);
        if (!errors.isEmpty()) {
            String errorMsg = StringUtils.collectionToDelimitedString(errors, "; ");
            logger.error("User update validation failed for ID {}: {}", userDTO.getId(), errorMsg);
            throw new UserException(errorMsg);
        }

        Optional<Role> role = roleRepository.findRoleByName(userDTO.getRoleName().toUpperCase());
        if (role.isEmpty()) {
            logger.error("Role not found: {}", userDTO.getRoleName().toUpperCase());
            throw new UserException("Role not found with name field: " + userDTO.getRoleName().toUpperCase());
        }

        Optional<User> userOptional = userRepository.findById(userDTO.getId());
        if (userOptional.isEmpty()) {
            logger.error("User not found for update with ID: {}", userDTO.getId());
            throw new UserException("User not found with id field: " + userDTO.getId());
        }
        User user = userOptional.get();

        // Check for email duplication only if the email is being changed
        if (!user.getEmail().equals(userDTO.getEmail())) {
            Optional<User> verifyDuplicated = userRepository.findUserByEmail(userDTO.getEmail());
            if (verifyDuplicated.isPresent()) {
                logger.warn("Attempted to update user ID {} with duplicate email: {}", userDTO.getId(), userDTO.getEmail());
                throw new UserException("User record does not permit duplicates for email field: " + userDTO.getEmail());
            }
            user.setEmail(userDTO.getEmail()); // Update email if changed
        }

        user.setName(userDTO.getName());
        // Consider if password update should be a separate endpoint/process
        // Only update password if provided and not empty?
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            user.setPassword(bCryptPasswordEncoder.encode(userDTO.getPassword()));
        }
        user.setRole(role.get());
        // Update timestamp? user.setTimeStamp(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        logger.info("User updated successfully with ID: {}", updatedUser.getId());
        return updatedUser.getId();
    }

    public void deleteUser(Long id) throws UserException {
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            logger.error("User not found for deletion with ID: {}", id);
            throw new UserException("User not found with id field: " + id);
        }
        userRepository.deleteById(id);
        logger.info("User deleted successfully with ID: {}", id);
    }

    public List<UserViewDTO> findUserViewByRoleName(String roleName) throws UserException {
        List<User> userList = userRepository.findUserByRoleName(roleName);
        if (userList.isEmpty()) {
            logger.warn("No users found with role name: {}", roleName);
            // Depending on requirements, maybe return empty list instead of throwing exception?
            // throw new UserException("User not found with role name field: " + roleName);
        }
        return userList.stream()
                .map(UserViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }

    // This registration method seems redundant if createUser is used via API.
    // If used internally, ensure password encoding happens.
    public User register(User user) {
        // Encrypt password before saving
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        logger.info("User registered internally with ID: {}", savedUser.getId());
        return savedUser;
    }

    public AuthResponse login(User user) {
        try {
            logger.info("Attempting authentication for user: {}", user.getEmail());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
            );

            if (authentication.isAuthenticated()) {
                logger.info("Authentication successful for user: {}", user.getEmail());
                Optional<User> existingUser = userRepository.findUserByEmail(user.getEmail());
                if (existingUser.isPresent()) {
                    User authenticatedUser = existingUser.get();
                    logger.info("Generating token for user ID: {}", authenticatedUser.getId());
                    // *** Pass user ID and email to generateToken ***
                    String token = jwtService.generateToken(authenticatedUser.getId(), authenticatedUser.getEmail());

                    return new AuthResponse(
                            token,
                            authenticatedUser.getId(),
                            authenticatedUser.getEmail(),
                            authenticatedUser.getRole().getName()
                    );
                }
                logger.error("User {} authenticated but not found in repository!", user.getEmail());
                throw new RuntimeException("User not found after authentication"); // Should ideally not happen
            }
            // Should not be reachable if authenticate throws exception on failure
            logger.warn("Authentication failed for user {} but no exception was thrown.", user.getEmail());
            throw new RuntimeException("Authentication failed");
        } catch (AuthenticationException e) {
            logger.warn("Authentication failed for user {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Invalid email or password", e); // Propagate specific auth failure
        } catch (Exception e) {
            // Catch other potential errors during login
            logger.error("Unexpected error during login process for user {}: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Login process failed", e);
        }
    }
}