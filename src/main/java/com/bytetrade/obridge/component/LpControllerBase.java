package com.bytetrade.obridge.component;

import java.math.BigInteger;

public class LpControllerBase {
    public String getHexString(String numStr) {

        if (numStr.startsWith("0x")) {
            return numStr;
        }

        return "0x" + new BigInteger(numStr).toString(16);
    }
}
