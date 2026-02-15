package com.example.user_service.user.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @Size(max = 255, message = "Name cannot exceed 255 characters")
    private String name;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    @AssertTrue(message = "Name cannot be blank")
    public boolean isNameNotBlankWhenPresent() {
        return name == null || !name.isBlank();
    }

    @AssertTrue(message = "Phone number cannot be blank")
    public boolean isPhoneNotBlankWhenPresent() {
        return phone == null || !phone.isBlank();
    }

    @AssertTrue(message = "At least one of name or phone must be provided")
    public boolean isAtLeastOneFieldProvided() {
        return (name != null && !name.isBlank()) || (phone != null && !phone.isBlank());
    }
}
