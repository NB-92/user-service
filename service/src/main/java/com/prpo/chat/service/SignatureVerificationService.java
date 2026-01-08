package com.prpo.chat.service;

import org.springframework.stereotype.Service;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;

@Service
public class SignatureVerificationService {

    private static final String PERSONAL_MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";

    public boolean verifySignature(String message, String signature, String walletAddress) {
        try {
            String recoveredAddress = recoverAddress(message, signature);
            return recoveredAddress != null &&
                    recoveredAddress.equalsIgnoreCase(walletAddress);
        } catch (Exception e) {
            return false;
        }
    }

    private String recoverAddress(String message, String signature) {
        try {
            String prefixedMessage = PERSONAL_MESSAGE_PREFIX + message.length() + message;
            byte[] messageHash = Hash.sha3(prefixedMessage.getBytes());

            byte[] signatureBytes = Numeric.hexStringToByteArray(signature);
            if (signatureBytes.length != 65) {
                return null;
            }

            byte v = signatureBytes[64];
            if (v < 27) {
                v += 27;
            }

            byte[] r = Arrays.copyOfRange(signatureBytes, 0, 32);
            byte[] s = Arrays.copyOfRange(signatureBytes, 32, 64);

            int recId = v - 27;

            int[] recoveryIds = { recId, recId ^ 1, 2, 3 };

            for (int id : recoveryIds) {
                BigInteger publicKey = Sign.recoverFromSignature(
                        id,
                        new ECDSASignature(new BigInteger(1, r), new BigInteger(1, s)),
                        messageHash);

                if (publicKey != null) {
                    String recoveredAddress = "0x" + Keys.getAddress(publicKey);
                    return recoveredAddress;
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
