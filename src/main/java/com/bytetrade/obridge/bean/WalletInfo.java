package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class WalletInfo {
    
    String name;

    List<String> balance;
}
