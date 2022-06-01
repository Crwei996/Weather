package com.weiwei.weather.gson;

import com.google.gson.annotations.SerializedName;

public class Basic {
    //使用注解的方式建立映射的关系
    @SerializedName("city")
    public String cityName;
    @SerializedName("id")
    public String weatherId;
    public Update update;
    public class Update{
        @SerializedName("loc")
        public String updateTime;
    }
}
