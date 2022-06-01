package com.weiwei.weather.gson;

import com.google.gson.annotations.SerializedName;
import com.weiwei.weather.db.City;

public class AQI {
    public AQICity city;
    public class AQICity{
        public String aqi;
        public String pm25;

    }

}
