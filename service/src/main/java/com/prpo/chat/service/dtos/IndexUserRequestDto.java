package com.prpo.chat.service.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IndexUserRequestDto {
    @NotBlank
    private String userId;

    @NotBlank
    private String username;
}
