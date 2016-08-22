package com.spaniard.model;

import java.util.Map;

/**
 * @author Alexander Nesterov
 * @version 1.0
 */
public class RequestData {

    private final String uri;
    private final Map<String, String> params;

    public RequestData(String uri, Map<String, String> params) {
        this.uri = uri;
        this.params = params;
    }

    public String getUri() {
        return uri;
    }

    public Map<String, String> getParams() {
        return params;
    }
}
