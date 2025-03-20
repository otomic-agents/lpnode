package com.bytetrade.obridge.component.utils;

import org.bitcoinj.core.Base58;
import java.math.BigInteger;

public class AddressHelper {

    public static String getHexAddress(String rawAddress, Integer chainId) {
        if (rawAddress.startsWith("0x")) {
            return rawAddress;
        }

        if (chainId == 501) {
            byte[] decodedBytes = Base58.decode(rawAddress);
            return bytesToHex(decodedBytes);
        }

        throw new IllegalArgumentException("Unsupported chainId for address conversion.");
    }

    public static String getDecimalAddress(String rawAddress, Integer chainId) {
        if (chainId == 501) {
            if (rawAddress.startsWith("0x")){
                String hexStr = rawAddress.substring(2);
                return new BigInteger(hexStr, 16).toString();
            }
            // Decode Base58 for chainId 501
            byte[] decodedBytes = Base58.decode(rawAddress);
            return new BigInteger(1, decodedBytes).toString();
        } else if (rawAddress.startsWith("0x")) {
            // Remove the "0x" prefix and convert hex to decimal
            String hexStr = rawAddress.substring(2);
            return new BigInteger(hexStr, 16).toString();
        } else {
            throw new IllegalArgumentException(
                    String.format("Unsupported address format for conversion,address:%s", rawAddress));
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return "0x" + hexString.toString();
    }
}
