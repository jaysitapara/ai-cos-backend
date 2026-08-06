package com.app.service.impl;

import com.app.common.ApiConstants;
import com.app.dto.request.UserCreateRequest;
import com.app.dto.response.UserResponse;
import com.app.entity.UserEntity;
import com.app.enums.UserRole;
import com.app.enums.UserStatus;
import com.app.exception.DuplicateResourceException;
import com.app.exception.ResourceNotFoundException;
import com.app.repository.UserRepository;
import com.app.service.UserService;
import com.app.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new DuplicateResourceException("A user with email '%s' already exists".formatted(email));
        }

        UserEntity user = UserEntity.builder()
            .publicId(UUID.randomUUID())
            .email(email)
            .fullName(request.fullName().trim())
            .passwordHash(passwordEncoder.encode("DefaultPassword123!"))
            .role(UserRole.ROLE_USER)
            .status(UserStatus.ACTIVE)
            .build();

        UserEntity saved = userRepository.save(user);
        log.info("Created user public_id={}", saved.getPublicId());

        return UserMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc().stream()
            .map(UserMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByPublicId(UUID publicId) {
        return UserMapper.toResponse(findActiveUser(publicId));
    }

    @Override
    @Transactional
    public void deleteUser(UUID publicId) {
        UserEntity user = findActiveUser(publicId);
        user.markDeleted(ApiConstants.SYSTEM_ACTOR);
        userRepository.save(user);
        log.info("Soft deleted user public_id={}", publicId);
    }

    private UserEntity findActiveUser(UUID publicId) {
        return userRepository.findByPublicIdAndDeletedAtIsNull(publicId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User with public_id '%s' was not found".formatted(publicId)));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
