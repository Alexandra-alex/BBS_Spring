package com.easybbs.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    //对象/数组等转JSON
    public static String convertObj2Json(Object obj) {
        return JSON.toJSONString(obj);
    }

    //JSON转对象
    public static <T> T convertJson2Obj(String json,Class<T> clazz) {
        return JSONObject.parseObject(json, clazz);
    }

    //字符串数组转集合对象
    public static <T> List<T> convertJsonArray2List(String json, Class<T> clazz) {
        return JSONArray.parseArray(json, clazz);
    }
}
