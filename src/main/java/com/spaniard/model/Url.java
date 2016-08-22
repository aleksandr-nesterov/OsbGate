package com.spaniard.model;

/**
 * @author Alexander Nesterov
 * @version 1.0
 */
public class Url {

    private final String host;
    private final int port;
    private final String uri;

    public Url(String host, int port, String uri) {
        this.host = host;
        this.port = port;
        this.uri = uri;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Url{");
        sb.append("host='").append(host).append('\'');
        sb.append(", port=").append(port);
        sb.append(", uri='").append(uri).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
