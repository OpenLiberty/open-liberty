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

import static com.ibm.ws.jdbc.fat.krb5.containers.KerberosContainer.KRB5_KDC_EXTERNAL;
import static com.ibm.ws.jdbc.fat.krb5.containers.KerberosContainer.KRB5_REALM;

import java.time.Duration;
import java.util.TimeZone;

import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import componenttest.containers.ImageBuilder;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;

public class DB2KerberosContainer extends Db2Container implements KerberosAuthContainer {

    private static final Class<?> c = DB2KerberosContainer.class;

    private static final DockerImageName DB2_KRB5 = ImageBuilder
                    .build("db2-krb5:12.1.2.0")
                    .getDockerImageName()
                    .asCompatibleSubstituteFor("icr.io/db2_community/db2");

    private static final String KRB5_USER = "dbuser";
    private static final String KRB5_PASS = "password";

    public DB2KerberosContainer(Network network) {
        super(DB2_KRB5);
        withNetwork(network);
        withNetworkAliases("db2");
        withCreateContainerCmdModifier(cmd -> {
            cmd.withHostName("db2");
        });
    }

    @Override
    protected void configure() {
        acceptLicense();

        // Superclass settings
        super.withUsername("db2inst1");
        super.withPassword("password");
        super.withDatabaseName("testdb");

        // Run as privilaged
        setPrivilegedMode(true);

        // Additional env variables
        withEnv("KRB5_REALM", KRB5_REALM);
        withEnv("KRB5_KDC", KRB5_KDC_EXTERNAL);
        withEnv("DB2_KRB5_PRINCIPAL", "db2srvc@EXAMPLE.COM");
        withEnv("KRB5_TRACE", "/dev/stdout");
        withEnv("TZ", TimeZone.getDefault().getID());

        // Wait strategy
        waitingFor(new LogMessageWaitStrategy()
                        .withRegEx("^.*SETUP SCRIPT COMPLETE.*$")
                        .withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN && !FATRunner.ARM_ARCHITECTURE ? 10 : 35)));

        // Logging
        withLogConsumer(new SimpleLogConsumer(c, "db2-krb5"));

        // Default configuration
        super.configure();
    }

    @Override
    public Db2Container withUsername(String username) {
        throw new UnsupportedOperationException("Username is hardcoded in container");
    }

    @Override
    public Db2Container withPassword(String password) {
        throw new UnsupportedOperationException("Password is hardcoded in container");
    }

    @Override
    public Db2Container withDatabaseName(String dbName) {
        throw new UnsupportedOperationException("DB name is hardcoded in container");
    }

    @Override
    public String getKerberosPrinciple() {
        return KRB5_USER + "@" + KerberosContainer.KRB5_REALM;
    }

    @Override
    public String getKerberosUsername() {
        return KRB5_USER;
    }

    @Override
    public String getKerberosPassword() {
        return KRB5_PASS;
    }
}
