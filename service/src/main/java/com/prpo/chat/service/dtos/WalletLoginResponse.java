package com.prpo.chat.service.dtos;

import lombok.Data;

@Data
public class WalletLoginResponse {
    private boolean needsRegistration;
    private UserDto user;

    public static WalletLoginResponse needsRegistration() {
        WalletLoginResponse response = new WalletLoginResponse();
        response.setNeedsRegistration(true);
        return response;
    }

    public static WalletLoginResponse success(UserDto user) {
        WalletLoginResponse response = new WalletLoginResponse();
        response.setNeedsRegistration(false);
        response.setUser(user);
        return response;
    }
}