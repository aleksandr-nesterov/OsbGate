package com.spaniard;

import com.spaniard.model.Url;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Alexander Nesterov
 * @version 1.0
 */
public class DynamicMappingHolder {
    public static final String SET_MAPPING = "/setMapping";
    public static final String CLEAR_MAPPING = "/clearMapping";
    public static final String REMOVE_KEY = "/removeMapping";
    public static final String HOST_KEY = "host";
    public static final String PORT_KEY = "port";
    public static final String PATH_KEY = "path";
    public static final String KEY = "key";

    public static final Map<String, Url> dynamicMapping = new ConcurrentHashMap<>();

}
