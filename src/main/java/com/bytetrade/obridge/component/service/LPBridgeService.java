package com.bytetrade.obridge.component.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bytetrade.obridge.bean.LPBridge;
import org.springframework.stereotype.Service;

@Service
public class LPBridgeService {
    private final Map<String, LPBridge> lpBridgesChannelMap = new ConcurrentHashMap<>();

    public Map<String, LPBridge> getLpBridgesChannelMap() {
        return lpBridgesChannelMap;
    }

    public void addLPBridge(String key, LPBridge lpBridge) {
        lpBridgesChannelMap.put(key, lpBridge);
    }

    public LPBridge getLPBridge(String key) {
        return lpBridgesChannelMap.get(key);
    }

    public void removeLPBridge(String key) {
        lpBridgesChannelMap.remove(key);
    }

    public boolean containsKey(String key) {
        return lpBridgesChannelMap.containsKey(key);
    }
}
