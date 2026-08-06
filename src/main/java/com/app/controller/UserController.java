package com.app.controller;

import com.app.common.ApiConstants;
import com.app.dto.request.UserCreateRequest;
import com.app.dto.response.UserResponse;
import com.app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiConstants.USERS_PATH)
@RequiredArgsConstructor
@Tag(name = "Users", description = "User onboarding and lookup endpoints")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create a user")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created"),
        @ApiResponse(responseCode = "400", description = "Validation failure"),
        @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping
    @Operation(summary = "List all active users")
    public ResponseEntity<List<UserResponse>> getUsers() {
        return ResponseEntity.ok(userService.getUsers());
    }

    @GetMapping("/{public_id}")
    @Operation(summary = "Fetch a single user by public id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "404", description = "User does not exist")
    })
    public ResponseEntity<UserResponse> getUser(@PathVariable("public_id") UUID publicId) {
        return ResponseEntity.ok(userService.getUserByPublicId(publicId));
    }

    @DeleteMapping("/{public_id}")
    @Operation(summary = "Soft delete a user by public id")
    public ResponseEntity<Void> deleteUser(@PathVariable("public_id") UUID publicId) {
        userService.deleteUser(publicId);
        return ResponseEntity.ok().build();
    }
}
