package com.easybbs.utils;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class CopyTools {
    //把一个集合copy成另一个集合(属性值)
    public static <T,S> List<T> copyList(List<S> sList, Class<T> clazz){
        List<T> list = new ArrayList<T>();
        for(S s :sList){
            T t = null;
            try{
                t = clazz.newInstance();
            }catch (Exception e){
                e.printStackTrace();
            }
            BeanUtils.copyProperties(s,t);
            list.add(t);
        }
        return list;
    }
    //把一个对象copy成另一个对象(属性值)
    public static <T,S> T copy(S s, Class<T> clazz){
        T t = null;
        try{
            t = clazz.newInstance();
        }catch (Exception e){
            e.printStackTrace();
        }
        BeanUtils.copyProperties(s,t);
        return t;
    }
}
