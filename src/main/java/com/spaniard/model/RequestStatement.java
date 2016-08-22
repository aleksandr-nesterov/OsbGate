package com.spaniard.model;

import io.netty.handler.codec.http.HttpRequest;

/**
 * @author Alexander Nesterov
 * @version 1.0
 */
public class RequestStatement {

    private final Url url;
    private final HttpRequest httpRequest;

    public RequestStatement(Url url, HttpRequest httpRequest) {
        this.url = url;
        this.httpRequest = httpRequest;
    }

    public Url getUrl() {
        return url;
    }

    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("RequestStatement{");
        sb.append("url=").append(url);
        sb.append(", httpRequest=").append(httpRequest);
        sb.append('}');
        return sb.toString();
    }
}
