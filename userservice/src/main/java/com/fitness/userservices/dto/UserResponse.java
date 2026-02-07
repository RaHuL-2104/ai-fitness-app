package com.fitness.userservices.dto;
import com.fitness.userservices.model.UserRole;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserResponse {
    private String id;
    private String keycloakID;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private UserRole role = UserRole.USER;
    private LocalDateTime createdAt;    
    private LocalDateTime updatedAt;
}
