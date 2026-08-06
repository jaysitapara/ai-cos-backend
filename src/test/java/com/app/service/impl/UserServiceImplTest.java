package com.app.service.impl;

import com.app.dto.request.UserCreateRequest;
import com.app.dto.response.UserResponse;
import com.app.entity.UserEntity;
import com.app.enums.UserStatus;
import com.app.exception.DuplicateResourceException;
import com.app.exception.ResourceNotFoundException;
import com.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createUserNormalizesEmailAndPersistsActiveUser() {
        when(userRepository.existsByEmailAndDeletedAtIsNull("jane.doe@example.com")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.createUser(
            new UserCreateRequest("  Jane Doe  ", "  Jane.Doe@Example.com "));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getEmail()).isEqualTo("jane.doe@example.com");
        assertThat(captor.getValue().getFullName()).isEqualTo("Jane Doe");
        assertThat(captor.getValue().getPublicId()).isNotNull();
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.email()).isEqualTo("jane.doe@example.com");
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        when(userRepository.existsByEmailAndDeletedAtIsNull("jane.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(
            new UserCreateRequest("Jane Doe", "jane.doe@example.com")))
            .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void getUserByPublicIdFailsWhenMissing() {
        UUID publicId = UUID.randomUUID();
        when(userRepository.findByPublicIdAndDeletedAtIsNull(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByPublicId(publicId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteUserSoftDeletesInsteadOfRemovingTheRow() {
        UUID publicId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
            .id(1L)
            .publicId(publicId)
            .email("jane.doe@example.com")
            .fullName("Jane Doe")
            .status(UserStatus.ACTIVE)
            .build();
        when(userRepository.findByPublicIdAndDeletedAtIsNull(publicId)).thenReturn(Optional.of(user));

        userService.deleteUser(publicId);

        assertThat(user.isDeleted()).isTrue();
        verify(userRepository).save(user);
        verify(userRepository, never()).delete(any(UserEntity.class));
    }
}
