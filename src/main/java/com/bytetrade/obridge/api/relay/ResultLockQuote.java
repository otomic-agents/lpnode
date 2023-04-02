package com.bytetrade.obridge.api.relay;

import com.bytetrade.obridge.base.Result;

import com.bytetrade.obridge.bean.PreBusiness;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ResultLockQuote extends Result{

    PreBusiness preBusiness;

    public ResultLockQuote(int _code, String _message) {
        super(_code, _message);
    }

    public ResultLockQuote(int _code, String _message, PreBusiness _preBusiness) {
        super(_code, _message);
        this.preBusiness = _preBusiness;
    }
    
}
