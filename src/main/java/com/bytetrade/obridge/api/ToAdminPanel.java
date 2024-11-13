package com.bytetrade.obridge.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bytetrade.obridge.base.Result;
import com.bytetrade.obridge.bean.LPBridge;
import com.bytetrade.obridge.component.LPController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@RestController
@RequestMapping("/lpnode/lpnode_admin_panel/")
public class ToAdminPanel {

    @Autowired
    LPController lpController;

    @PostMapping("config_lp")
    public Result configLP(
            @RequestBody List<LPBridge> bridges) {
        log.info("bridges:" + bridges.toString());
        boolean isSucceed = lpController.updateConfig(bridges);
        if (isSucceed) {
            return new Result(200, "config succeed");
        } else {
            return new Result(30206, "config failed");
        }
    }

    @GetMapping("list_bridge")
    public ResponseEntity<String> listBridge() {
        String bridgeInfoStr = lpController.printBridgeList();
        return ResponseEntity.ok(bridgeInfoStr);
    }
}
