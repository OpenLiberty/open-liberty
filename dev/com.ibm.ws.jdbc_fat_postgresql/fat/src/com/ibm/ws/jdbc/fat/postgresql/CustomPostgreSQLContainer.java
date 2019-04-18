package com.ibm.ws.jdbc.fat.postgresql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.testcontainers.containers.PostgreSQLContainer;

public class CustomPostgreSQLContainer<SELF extends CustomPostgreSQLContainer<SELF>> extends PostgreSQLContainer<SELF> {

    private final Map<String, String> options = new HashMap<>();

    public CustomPostgreSQLContainer(String img) {
        super(img);
    }

    /**
     * Add additional configuration options that should be used for this container.
     *
     * @param key   The PostgreSQL configuration option key. For example: "max_connections"
     * @param value The PostgreSQL configuration option value. For example: "200"
     * @return this
     */
    public SELF withConfigOption(String key, String value) {
        if (key == null) {
            throw new java.lang.NullPointerException("key marked @NonNull but is null");
        }
        if (value == null) {
            throw new java.lang.NullPointerException("value marked @NonNull but is null");
        }
        options.put(key, value);
        return self();
    }

    @Override
    protected void configure() {
        super.configure();
        List<String> command = new ArrayList<>();
        command.add("postgres");
        command.add("-c");
        command.add("fsync=off");
        for (Entry<String, String> e : options.entrySet()) {
            command.add("-c");
            command.add(e.getKey() + '=' + e.getValue());
        }
        setCommand(command.toArray(new String[command.size()]));
    }

}
