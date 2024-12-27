package com.bytetrade.obridge.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class JsonUtils {
    public static String toCompactJsonString(Object object) {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES) // 驼峰转下划线
                .create();

        
        String json = gson.toJson(object);


        JsonElement jsonElement = JsonParser.parseString(json);
        return gson.toJson(jsonElement);
    }
}