package com.bytetrade.obridge.bean;

import java.util.List;

import com.bytetrade.obridge.bean.LPBridge;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LPConfigCache {
    List<LPBridge> bridges;
}