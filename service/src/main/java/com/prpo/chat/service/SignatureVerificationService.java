package com.prpo.chat.service;

import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;

@Service
public class SignatureVerificationService {

    public boolean verifySignature(String message, String signature, String walletAddress) {
        try {
            byte[] signatureBytes = Numeric.hexStringToByteArray(signature);
            if (signatureBytes.length != 65) {
                return false;
            }

            byte v = signatureBytes[64];
            if (v < 27) {
                v += 27;
            }

            Sign.SignatureData signatureData = new Sign.SignatureData(
                    v,
                    Arrays.copyOfRange(signatureBytes, 0, 32),
                    Arrays.copyOfRange(signatureBytes, 32, 64));

            BigInteger publicKey = Sign.signedPrefixedMessageToKey(message.getBytes(), signatureData);
            String recoveredAddress = "0x" + Keys.getAddress(publicKey);

            return recoveredAddress.equalsIgnoreCase(walletAddress);
        } catch (Exception e) {
            return false;
        }
    }
}
