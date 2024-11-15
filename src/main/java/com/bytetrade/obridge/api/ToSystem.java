package com.bytetrade.obridge.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bytetrade.obridge.base.Result;
import com.bytetrade.obridge.component.LPController;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/lpnode/system/")
public class ToSystem {

    @Autowired
    LPController lpController;

    @GetMapping(value = "up_status", produces = "application/json;charset=UTF-8")
    public Result upStatus() {
        return new Result(200, "ok");
    }
}
