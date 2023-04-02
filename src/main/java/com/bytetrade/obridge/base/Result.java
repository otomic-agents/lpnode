package com.bytetrade.obridge.base;

public class Result {

    //32001 function not ready
    private int code;
    private String message;

    public Result(int _code, String _message) {
        this.code = _code;
        this.message = _message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
