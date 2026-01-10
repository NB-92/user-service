package com.prpo.chat.service.dtos;

import lombok.Data;

@Data
public class WalletRegisterRequest {
    private String walletAddress;
    private String signature;
    private String message;
    private String username;
}