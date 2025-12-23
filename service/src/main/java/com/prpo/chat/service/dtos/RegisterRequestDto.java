package com.prpo.chat.service.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDto {
    @Email
    @NotBlank
    private String email;

    @Size(min = 5, max = 24)
    private String username;

    @Size(min = 8, max = 128)
    private String password;
}
