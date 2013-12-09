package com.github.rosjava.android_apps.application_management.rapp_manager;

import java.util.LinkedHashMap;

/**
 * Just to provide a get method with default value to LinkedHashMap
 * Created by jorge on 11/26/13.
 */
public class AppRemappings extends LinkedHashMap<String, String> {
    public String get(String from) {
        return super.containsKey(from) ? super.get(from) : from;
    }

    public String get(String from, String to) {
        return super.containsKey(from) ? super.get(from) : to;
    }
}
