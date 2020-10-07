/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.krb5.containers;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.OutputFrame;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jdbc.fat.krb5.FATSuite;

public class PostgresKerberosContainer extends PostgreSQLContainer<PostgresKerberosContainer> {

    private static final Class<?> c = PostgresKerberosContainer.class;
    private static final Path reuseCache = Paths.get("..", "..", "cache", "postgres.properties");
    // NOTE: If this is ever updated, don't forget to push to docker hub, but DO NOT overwrite existing versions
    private static final String IMAGE = "aguibert/krb5-postgresql:1.0";

    public static final int PG_PORT = 5432;

    private boolean reused = false;
    private String reused_hostname;
    private int reused_port;
    private final Map<String, String> options = new HashMap<>();

    public PostgresKerberosContainer(Network network) {
        super(IMAGE);
        withNetwork(network);

        withNetworkAliases("postgresql");
        withCreateContainerCmdModifier(cmd -> {
            cmd.withHostName("postgresql");
        });
        if (!options.containsKey("fsync"))
            withConfigOption("fsync", "off");
        List<String> command = new ArrayList<>();
        for (Entry<String, String> e : options.entrySet()) {
            command.add("-c");
            command.add(e.getKey() + '=' + e.getValue());
        }
        setCommand(command.toArray(new String[command.size()]));
        withUsername("nonkrbuser");
        withPassword("password");
        withDatabaseName("pg");

        withEnv("POSTGRES_HOST_AUTH_METHOD", "gss");
        withEnv("KRB5_KTNAME", "/etc/krb5.keytab");
        withEnv("KRB5_TRACE", "/dev/stdout");

        withExposedPorts(PG_PORT);
        withLogConsumer(PostgresKerberosContainer::log);
        withReuse(true);
    }

    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(PostgresKerberosContainer.class, "[PG]", msg);
    }

    @Override
    public void start() {
        String dockerHostIp = DockerClientFactory.instance().dockerHostIpAddress();
        withEnv("EXTERNAL_HOSTNAME", dockerHostIp);
        Log.info(c, "start", "Using EXTERNAL_HOSTNAME=" + dockerHostIp);

        if (hasCachedContainers()) {
            // If this is a local run and a cache file exists, that means a container is already running
            // and we can just read the host/port from the cache file
            Log.info(c, "start", "Found existing container cache file. Skipping container start.");
            Properties props = new Properties();
            try {
                props.load(new FileInputStream(reuseCache.toFile()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            reused = true;
            reused_hostname = props.getProperty("postgresql.hostname");
            reused_port = Integer.valueOf(props.getProperty("postgresql.port"));
            Log.info(c, "start", "Found existing container at host = " + reused_hostname);
            Log.info(c, "start", "Found existing container on port = " + reused_port);
            return;
        }

        super.start();

        if (FATSuite.REUSE_CONTAINERS) {
            Log.info(c, "start", "Saving properties for future runs at: " + reuseCache.toAbsolutePath());
            try {
                Files.createDirectories(reuseCache.getParent());
                Properties props = new Properties();
                if (reuseCache.toFile().exists()) {
                    try (FileInputStream fis = new FileInputStream(reuseCache.toFile())) {
                        props.load(fis);
                    }
                }
                props.setProperty("postgresql.hostname", getContainerIpAddress());
                props.setProperty("postgresql.port", "" + getMappedPort(PG_PORT));
                props.store(new FileWriter(reuseCache.toFile()), "Generated by FAT run");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void stop() {
        if (FATSuite.REUSE_CONTAINERS) {
            Log.info(c, "stop", "Leaving container running so it can be used in later runs");
            return;
        } else {
            Log.info(c, "stop", "Stopping container");
            super.stop();
        }
    }

    @Override
    public Integer getMappedPort(int originalPort) {
        return (reused && originalPort == PG_PORT) ? reused_port : super.getMappedPort(originalPort);
    }

    /**
     * Add additional configuration options that should be used for this container.
     *
     * @param key   The PostgreSQL configuration option key. For example: "max_connections"
     * @param value The PostgreSQL configuration option value. For example: "200"
     * @return this
     */
    public PostgresKerberosContainer withConfigOption(String key, String value) {
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
    public String getContainerIpAddress() {
        return reused ? reused_hostname : super.getContainerIpAddress();
    }

    public String getKerberosUsername() {
        return "pguser@" + KerberosContainer.KRB5_REALM;
    }

    public String getKerberosPassword() {
        return "password";
    }

    private static boolean hasCachedContainers() {
        if (!FATSuite.REUSE_CONTAINERS)
            return false;
        if (!reuseCache.toFile().exists())
            return false;
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(reuseCache.toFile())) {
            props.load(fis);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return props.containsKey("postgresql.hostname") &&
               props.containsKey("postgresql.port");
    }

}
