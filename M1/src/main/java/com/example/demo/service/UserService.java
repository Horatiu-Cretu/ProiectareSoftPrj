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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    public List<UserViewDTO> findAllUserView() {
        return userRepository.findAll().stream()
                .map(user -> {
                    UserViewDTO dto = UserViewBuilder.generateDTOFromEntity(user);
                    dto.setBlocked(user.isBlocked());
                    dto.setBlockedReason(user.getBlockedReason());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public UserViewDTO findUserViewById(Long id) throws UserException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("User not found with id: {}", id);
                    return new UserException("User not found with id field: " + id);
                });
        UserViewDTO dto = UserViewBuilder.generateDTOFromEntity(user);
        dto.setBlocked(user.isBlocked());
        dto.setBlockedReason(user.getBlockedReason());
        return dto;
    }

    public UserViewDTO findUserViewByEmail(String email) throws UserException {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User not found with email: {}", email);
                    return new UserException("User not found with email field: " + email);
                });
        UserViewDTO dto = UserViewBuilder.generateDTOFromEntity(user);
        dto.setBlocked(user.isBlocked());
        dto.setBlockedReason(user.getBlockedReason());
        return dto;
    }


    public Long createUser(UserDTO userDTO) throws UserException {
        List<String> errors = UserFieldValidator.validateInsertOrUpdate(userDTO);
        if (!errors.isEmpty()) {
            String errorMsg = StringUtils.collectionToDelimitedString(errors, "; ");
            logger.error("User creation validation failed: {}", errorMsg);
            throw new UserException(errorMsg);
        }

        Role role = roleRepository.findRoleByName(userDTO.getRoleName().toUpperCase())
                .orElseThrow(() -> {
                    logger.error("Role not found: {}", userDTO.getRoleName().toUpperCase());
                    return new UserException("Role not found with name field: " + userDTO.getRoleName().toUpperCase());
                });


        if (userRepository.findUserByEmail(userDTO.getEmail()).isPresent()) {
            logger.warn("Attempted to create user with duplicate email: {}", userDTO.getEmail());
            throw new UserException("User record does not permit duplicates for email field: " + userDTO.getEmail());
        }

        User userSave = UserBuilder.generateEntityFromDTO(userDTO, role);
        userSave.setPassword(bCryptPasswordEncoder.encode(userSave.getPassword()));
        userSave.setTimeStamp(LocalDateTime.now());

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

        Role role = roleRepository.findRoleByName(userDTO.getRoleName().toUpperCase())
                .orElseThrow(() -> {
                    logger.error("Role not found: {}", userDTO.getRoleName().toUpperCase());
                    return new UserException("Role not found with name field: " + userDTO.getRoleName().toUpperCase());
                });

        User user = userRepository.findById(userDTO.getId())
                .orElseThrow(() -> {
                    logger.error("User not found for update with ID: {}", userDTO.getId());
                    return new UserException("User not found with id field: " + userDTO.getId());
                });

        if (!user.getEmail().equals(userDTO.getEmail())) {
            if (userRepository.findUserByEmail(userDTO.getEmail()).isPresent()) {
                logger.warn("Attempted to update user ID {} with duplicate email: {}", userDTO.getId(), userDTO.getEmail());
                throw new UserException("User record does not permit duplicates for email field: " + userDTO.getEmail());
            }
            user.setEmail(userDTO.getEmail());
        }

        user.setName(userDTO.getName());
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            user.setPassword(bCryptPasswordEncoder.encode(userDTO.getPassword()));
        }
        user.setRole(role);

        User updatedUser = userRepository.save(user);
        logger.info("User updated successfully with ID: {}", updatedUser.getId());
        return updatedUser.getId();
    }

    @Transactional
    public void deleteUser(Long id) throws UserException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found for deletion with ID: {}", id);
                    return new UserException("User not found with id field: " + id);
                });
        userRepository.delete(user);
        logger.info("User deleted successfully with ID: {}", id);
    }

    public List<UserViewDTO> findUserViewByRoleName(String roleName) throws UserException {
        List<User> userList = userRepository.findUserByRoleName(roleName.toUpperCase());
        if (userList.isEmpty()) {
            logger.warn("No users found with role name: {}", roleName);
        }
        return userList.stream()
                .map(user -> {
                    UserViewDTO dto = UserViewBuilder.generateDTOFromEntity(user);
                    dto.setBlocked(user.isBlocked());
                    dto.setBlockedReason(user.getBlockedReason());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public User register(User user) throws UserException {
        if (userRepository.findUserByEmail(user.getEmail()).isPresent()) {
            throw new UserException("Email already exists: " + user.getEmail());
        }
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) {
            Role defaultRole = roleRepository.findByName("USER");
            if(defaultRole == null) {
                defaultRole = roleRepository.save(new Role("USER"));
            }
            user.setRole(defaultRole);
        }
        user.setTimeStamp(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        logger.info("User registered internally with ID: {}", savedUser.getId());
        return savedUser;
    }

    public AuthResponse login(User userLoginAttempt) throws UserException {
        try {
            logger.info("Attempting authentication for user: {}", userLoginAttempt.getEmail());

            User user = userRepository.findUserByEmail(userLoginAttempt.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

            if (user.isBlocked()) {
                logger.warn("Login attempt for blocked user {}: {}", user.getEmail(), user.getBlockedReason());
                throw new DisabledException("Your account has been blocked. Reason: " + user.getBlockedReason());
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userLoginAttempt.getEmail(), userLoginAttempt.getPassword())
            );


            logger.info("Authentication successful for user: {}", user.getEmail());
            logger.info("Generating token for user ID: {}", user.getId());
            String token = jwtService.generateToken(user.getId(), user.getEmail());

            return new AuthResponse(
                    token,
                    user.getId(),
                    user.getEmail(),
                    user.getRole().getName()
            );

        } catch (AuthenticationException e) {
            logger.warn("Authentication failed for user {}: {}", userLoginAttempt.getEmail(), e.getMessage());
            if (e instanceof DisabledException) {
                throw new UserException(e.getMessage());
            }
            throw new UserException("Invalid email or password.");
        } catch (Exception e) {
            logger.error("Unexpected error during login process for user {}: {}", userLoginAttempt.getEmail(), e.getMessage(), e);
            throw new UserException("Login process failed due to an unexpected error.");
        }
    }

    @Transactional
    public void blockUser(Long targetUserId, String reason, Long actionPerformingAdminId) throws UserException {
        User admin = userRepository.findById(actionPerformingAdminId)
                .orElseThrow(() -> new UserException("Performing admin user not found."));
        if (!"ADMIN".equalsIgnoreCase(admin.getRole().getName())) {
            throw new UserException("User " + actionPerformingAdminId + " is not authorized to block users.");
        }

        User userToBlock = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserException("User to block not found with ID: " + targetUserId));

        if (userToBlock.isBlocked()) {
            logger.info("User {} is already blocked. Updating reason.", targetUserId);
        }

        userToBlock.setBlocked(true);
        userToBlock.setBlockedReason(reason);
        userToBlock.setBlockedAt(LocalDateTime.now());
        userToBlock.setBlockedByAdminId(actionPerformingAdminId);
        userRepository.save(userToBlock);
        logger.info("User {} blocked by admin {} with reason: {}", targetUserId, actionPerformingAdminId, reason);
    }

    @Transactional
    public void unblockUser(Long targetUserId, Long actionPerformingAdminId) throws UserException {
        User admin = userRepository.findById(actionPerformingAdminId)
                .orElseThrow(() -> new UserException("Performing admin user not found."));
        if (!"ADMIN".equalsIgnoreCase(admin.getRole().getName())) {
            throw new UserException("User " + actionPerformingAdminId + " is not authorized to unblock users.");
        }

        User userToUnblock = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserException("User to unblock not found with ID: " + targetUserId));

        if (!userToUnblock.isBlocked()) {
            logger.info("User {} is already unblocked.", targetUserId);
            return;
        }

        userToUnblock.setBlocked(false);
        userToUnblock.setBlockedReason(null);
        userToUnblock.setBlockedAt(null);
        userRepository.save(userToUnblock);
        logger.info("User {} unblocked by admin {}", targetUserId, actionPerformingAdminId);
    }

    public User getUserById(Long userId) throws UserException {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found with ID: " + userId));
    }
}
