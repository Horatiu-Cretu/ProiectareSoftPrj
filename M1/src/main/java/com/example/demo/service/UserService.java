package com.example.demo.service;

import com.example.demo.builder.userbuilder.UserBuilder;
import com.example.demo.builder.userbuilder.UserViewBuilder;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    @Autowired
    private final RoleRepository roleRepository;

    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

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
            throw new UserException("User not found with id field: " + id);
        }
        return UserViewBuilder.generateDTOFromEntity(user.get());
    }

    public UserViewDTO findUserViewByEmail(String email) throws UserException {
        Optional<User> user = userRepository.findUserByEmail(email);

        if (user.isEmpty()) {
            throw new UserException("User not found with email field: " + email);
        }
        return UserViewBuilder.generateDTOFromEntity(user.get());
    }


    public Long createUser(UserDTO userDTO) throws UserException {
        List<String> errors = UserFieldValidator.validateInsertOrUpdate(userDTO);

        if (!errors.isEmpty()) {
            throw new UserException(StringUtils.collectionToDelimitedString(errors, "\n"));
        }

        Optional<Role> role = roleRepository.findRoleByName(userDTO.getRoleName().toUpperCase());

        if (role.isEmpty()) {
            throw new UserException("Role not found with name field: " + userDTO.getRoleName().toUpperCase());
        }

        Optional<User> user = userRepository.findUserByEmail(userDTO.getEmail());
        if (user.isPresent()) {
            throw new UserException("User record does not permit duplicates for email field: " + userDTO.getEmail());
        }

        User userSave = UserBuilder.generateEntityFromDTO(userDTO, role.get());

        return userRepository.save(userSave).getId();
    }

    public Long updateUser(UserDTO userDTO) throws UserException {
        List<String> errors = UserFieldValidator.validateInsertOrUpdate(userDTO);

        if (!errors.isEmpty()) {
            throw new UserException(StringUtils.collectionToDelimitedString(errors, "\n"));
        }

        Optional<Role> role = roleRepository.findRoleByName(userDTO.getRoleName().toUpperCase());

        if (role.isEmpty()) {
            throw new UserException("Role not found with name field: " + userDTO.getRoleName().toUpperCase());
        }

        Optional<User> user = userRepository.findById(userDTO.getId());
        if (user.isEmpty()) {
            throw new UserException("User not found with id field: " + userDTO.getId());
        }


        if (!user.get().getEmail().equals(userDTO.getEmail())) {
            Optional<User> verifyDuplicated = userRepository.findUserByEmail(userDTO.getEmail());
            if (verifyDuplicated.isPresent()) {
                throw new UserException("User record does not permit duplicates for email field: " + userDTO.getEmail());
            }
        }

        user.get().setName(userDTO.getName());
        user.get().setEmail(userDTO.getEmail());
        user.get().setPassword(userDTO.getPassword());
        user.get().setRole(role.get());

        return userRepository.save(user.get()).getId();
    }

    public void deleteUser(Long id) throws UserException {

        Optional<User> user = userRepository.findById(id);

        if (user.isEmpty()) {
            throw new UserException("User not found with id field: " + id);
        }

        this.userRepository.deleteById(id);
    }

    public List<UserViewDTO> findUserViewByRoleName(String roleName) throws UserException {
        List<User> userList = userRepository.findUserByRoleName(roleName);

        if (userList.isEmpty()) {
            throw new UserException("User not found with role name field: " + roleName);
        }
        return userList.stream()
                .map(UserViewBuilder::generateDTOFromEntity)
                .collect(Collectors.toList());
    }

    public User register(User user) {
        // Encrypt password before saving
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        // Save user to database
        return userRepository.save(user);
    }

    public String login(User user) {
        try {
            // Authenticate using email and password
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
            );

            // If authentication is successful, generate and return token
            if (authentication.isAuthenticated()) {
                Optional<User> existingUser = userRepository.findUserByEmail(user.getEmail());

                if (existingUser.isPresent()) {
                    return jwtService.generateToken(existingUser.get().getEmail());
                }

                throw new RuntimeException("User not found");
            }

            throw new RuntimeException("Authentication failed");
        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid email or password", e);
        }
    }


}

