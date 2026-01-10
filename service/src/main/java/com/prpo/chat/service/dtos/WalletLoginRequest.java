package com.prpo.chat.service.dtos;

import lombok.Data;

@Data
public class WalletLoginRequest {
    private String walletAddress;
    private String signature;
    private String message;
}