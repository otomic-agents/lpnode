package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Accessors(chain = true)
public class KycInfo {

    String address;

    String birthday;

    String country;

    String email;

    String first_name;

    String gender;

    String id_end_image;

    String id_front_image;

    String id_number;

    String id_type;

    String image1;

    String image2;

    String last_name;

    String phone;

    String username;

    String identification_photo_1;

    String identification_photo_2;

    String identification_photo_3;

    String identification_photo_4;

    String did;

    String status;

    String vp;
}
