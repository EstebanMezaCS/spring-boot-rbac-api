package com.rbac.api.dto;

import com.rbac.api.entity.Role;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Dtos {

    // ==================== Login ====================
    
    public static class LoginRequest {
        private String username;
        private String password;

        public LoginRequest() {}
        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginResponse {
        private UUID id;
        private String username;
        private String name;
        private String email;
        private Role role;
        private List<String> permissions;
        private String message;
        private String token;              // signed JWT to send as "Authorization: Bearer <token>"
        private String tokenType = "Bearer";
        private long expiresIn;            // token lifetime in seconds

        public LoginResponse() {}

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
        public List<String> getPermissions() { return permissions; }
        public void setPermissions(List<String> permissions) { this.permissions = permissions; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }
        public long getExpiresIn() { return expiresIn; }
        public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
    }

    // ==================== Register ====================

    public static class RegisterRequest {
        private String username;
        private String password;
        private String name;
        private String email;

        public RegisterRequest() {}

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    // ==================== User Response ====================

    public static class UserResponse {
        private UUID id;
        private String username;
        private String name;
        private String email;
        private Role role;
        private List<String> permissions;
        private boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime lastLogin;

        public UserResponse() {}

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
        public List<String> getPermissions() { return permissions; }
        public void setPermissions(List<String> permissions) { this.permissions = permissions; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getLastLogin() { return lastLogin; }
        public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    }

    // ==================== Create User ====================

    public static class CreateUserRequest {
        private String username;
        private String password;
        private String name;
        private String email;
        private Role role;

        public CreateUserRequest() {}

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
    }

    // ==================== Update User ====================

    public static class UpdateUserRequest {
        private String name;
        private String email;
        private String password;
        private Boolean isActive;

        public UpdateUserRequest() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }

    // ==================== Change Role ====================

    public static class ChangeRoleRequest {
        private Role role;

        public ChangeRoleRequest() {}

        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
    }

    // ==================== API Response Wrapper ====================

    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public ApiResponse() {}

        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(true, null, data);
        }

        public static <T> ApiResponse<T> success(String message, T data) {
            return new ApiResponse<>(true, message, data);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
    }

    // ==================== Error Response ====================

    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private LocalDateTime timestamp;

        public ErrorResponse() {}

        public ErrorResponse(int status, String error, String message) {
            this.status = status;
            this.error = error;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }

        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
