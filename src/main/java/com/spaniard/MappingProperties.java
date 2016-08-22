package com.spaniard;

import com.spaniard.model.Url;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * @author Alexander Nesterov
 * @version 1.0
 */
public class MappingProperties {

    private static final String MAPPING = ApplicationProperties.getInstance().getPathToStaticMapping();

    private static final MappingProperties mappingProperties = new MappingProperties();

    private final Properties properties;

    private MappingProperties() {
        this.properties = new Properties();
        load();
    }

    public static MappingProperties getInstance() {
        return mappingProperties;
    }

    public Url getUrl(final String key) {
        if (!properties.containsKey(key)) return null;

        final String value = properties.get(key).toString();
        URI uri = null;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return new Url(uri.getHost(), uri.getPort(), uri.getPath());
    }

    public void reload() {
        synchronized (properties) {
            properties.clear();
            load();
        }
    }

    private void load() {
        try (InputStream in = new FileInputStream(MAPPING)){
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
