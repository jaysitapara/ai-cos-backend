package com.app.service;

import com.app.dto.request.UserCreateRequest;
import com.app.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse createUser(UserCreateRequest request);

    List<UserResponse> getUsers();

    UserResponse getUserByPublicId(UUID publicId);

    void deleteUser(UUID publicId);
}
