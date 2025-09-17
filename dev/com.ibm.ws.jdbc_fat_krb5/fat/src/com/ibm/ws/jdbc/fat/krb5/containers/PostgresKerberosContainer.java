/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.krb5.containers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.ImageBuilder;
import componenttest.containers.SimpleLogConsumer;

public class PostgresKerberosContainer extends PostgreSQLContainer<PostgresKerberosContainer> {

    private static final Class<?> c = PostgresKerberosContainer.class;

    private static final DockerImageName POSTGRES_KRB5 = ImageBuilder
                    .build("postgres-krb5:17.0.0.1")
                    .getDockerImageName()
                    .asCompatibleSubstituteFor("postgres");

    public static final int PG_PORT = 5432;

    private final Map<String, String> options = new HashMap<>();

    private static final String KEYTAB_FILE = "/etc/krb5.keytab";
    private static final String KERBEROS_TRACE = "/dev/stdout";
    private static final String AUTH_METHOD = "gss";

    public PostgresKerberosContainer(Network network) {
        super(POSTGRES_KRB5);

        // Network
        withNetwork(network);
        withNetworkAliases("postgresql");
        withCreateContainerCmdModifier(cmd -> {
            cmd.withHostName("postgresql");
        });
        withExposedPorts(PG_PORT);

        // Authentication
        withUsername("nonkrbuser");
        withPassword("password");

        // Database
        withDatabaseName("pg");

        // Environment
        withEnv("POSTGRES_HOST_AUTH_METHOD", AUTH_METHOD);
        withEnv("KRB5_KTNAME", KEYTAB_FILE);
        withEnv("KRB5_TRACE", KERBEROS_TRACE);

        // Logging
        withLogConsumer(new SimpleLogConsumer(c, "postgre-krb5"));

        // Configuration

        /**
         * Performance improvement
         */
        withConfigOption("fsync", "off");

        /**
         * Since PostgreSQL 13 - krb_server_keyfile is set to a default location of
         * FILE:/usr/local/pgsql/etc/krb5.keytab
         * instead of using the kerberos environment variable KRB5_KTNAME.
         */
        withConfigOption("krb_server_keyfile", KEYTAB_FILE);
    }

    @Override
    public void configure() {
        super.configure();

        List<String> command = new ArrayList<>();
        for (Entry<String, String> e : options.entrySet()) {
            command.add("-c");
            command.add(e.getKey() + '=' + e.getValue());
        }
        setCommand(command.toArray(new String[command.size()]));
    }

    @Override
    public void start() {
        String dockerHostIp = DockerClientFactory.instance().dockerHostIpAddress();
        withEnv("EXTERNAL_HOSTNAME", dockerHostIp);
        Log.info(c, "start", "Using EXTERNAL_HOSTNAME=" + dockerHostIp);
        super.start();
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

    public String getKerberosUsername() {
        return "pguser@" + KerberosContainer.KRB5_REALM;
    }

    public String getKerberosPassword() {
        return "password";
    }
}
