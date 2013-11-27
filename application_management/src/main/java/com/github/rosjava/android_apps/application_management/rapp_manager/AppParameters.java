package com.github.rosjava.android_apps.application_management.rapp_manager;

import java.util.LinkedHashMap;

/**
 * Just to provide a get method with default value to LinkedHashMap
 * Created by jorge on 11/26/13.
 */
public class AppParameters extends LinkedHashMap<String, Object>  {
    public AppParameters() { super(); } // Required by snake yaml

    public Object get(String key, Object def) {
        return super.containsKey(key) ? super.get(key) : def;
    }
}
