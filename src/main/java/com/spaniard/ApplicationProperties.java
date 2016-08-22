package com.spaniard;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Alexander Nesterov
 * @version 1.0
 */
public class ApplicationProperties {

    private static final String APPLICATION_PROPERTIES = "application.properties";
    private static final String OSB_SERVER = "osb.server";
    private static final String OSB_PORT = "osb.port";
    private static final String OSB_GATE_PORT = "osb.gate.port";
    private static final String PATH_TO_STATIC_MAPPING = "path.to.static.mapping";
    private static final String SCAN_MAPPING_TIMEOUT = "scan.mapping.timeout";

    private static final ApplicationProperties applicationProperties = new ApplicationProperties();

    private final Properties properties;

    private ApplicationProperties() {
        this.properties = new Properties();
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(APPLICATION_PROPERTIES)) {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ApplicationProperties getInstance() {
        return applicationProperties;
    }

    public String getOsbHost() {
        return properties.get(OSB_SERVER).toString();
    }

    public int getOsbPort() {
        return Integer.valueOf(properties.get(OSB_PORT).toString());
    }

    public int getPort() {
        return Integer.valueOf(properties.get(OSB_GATE_PORT).toString());
    }

    public String getPathToStaticMapping() {
        return properties.getProperty(PATH_TO_STATIC_MAPPING);
    }

    public int getScanMappingTimeout() {
        return Integer.valueOf(properties.getProperty(SCAN_MAPPING_TIMEOUT).toString());
    }

}
