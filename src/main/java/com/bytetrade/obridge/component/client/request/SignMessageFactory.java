package com.bytetrade.obridge.component.client.request;

public class SignMessageFactory {
    public static RequestSignMessage createSignMessage(Integer chainId) {
        switch (chainId) {
            case 9006:
                return new RequestSignMessage712();
            case 9000:
                return new RequestSignMessage712();
            case 60:
                return new RequestSignMessage712();
            case 614:
                return new RequestSignMessage712();
            case 966:
                return new RequestSignMessage712();
            case 397:
                return new RequestSignMessage712();
            case 501:
                return new RequestSignMessageSolana();
            default:
                throw new IllegalArgumentException("Unsupported chainId");
        }
    }
}
