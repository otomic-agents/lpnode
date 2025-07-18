package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@AllArgsConstructor
public class BusinessFullData<T extends BusinessFullData<T>> {

    PreBusiness preBusiness;

    Business business;

    public T setPreBusiness(PreBusiness preBusiness) {
        this.preBusiness = preBusiness;
        return (T) this;
    }

    public T setBusiness(Business business) {
        this.business = business;
        return (T) this;
    }


}