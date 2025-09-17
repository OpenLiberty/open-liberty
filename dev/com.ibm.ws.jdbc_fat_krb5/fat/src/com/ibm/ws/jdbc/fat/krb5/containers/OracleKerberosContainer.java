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

import org.testcontainers.containers.Network;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import componenttest.containers.ImageBuilder;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;

/**
 * Custom Oracle Kerberos Container class
 */
public class OracleKerberosContainer extends OracleContainer {

    private static final Class<?> c = OracleKerberosContainer.class;

    /*
     * TODO: https://github.com/testcontainers/testcontainers-java/pull/10263
     * If this ever get's merged consider passing the future (RemoteDockerImage)
     * to the parent constructor so we can lazily start this image.
     */
    private static final DockerImageName ORACLE_KRB5 = ImageBuilder
                    .build("oracle-krb5:23.0.0.1-full-faststart")
                    .getDockerImageName()
                    .asCompatibleSubstituteFor("gvenzl/oracle-free");

    public OracleKerberosContainer(Network network) {
        super(ORACLE_KRB5);

        // Network
        withNetwork(network);
        withNetworkAliases("oracle");
        withCreateContainerCmdModifier(cmd -> {
            cmd.withHostName("oracle");
        });

        // Authentication
        super.withPassword("oracle");

        // Connections
        usingSid(); //Maintain current behavior of connecting with SID instead of pluggable database

        // Startup
        withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 3 : 25));

        // Logging
        withLogConsumer(new SimpleLogConsumer(c, "oracle-krb5"));

        // Environment
        withEnv("KRB5_REALM", KRB5_REALM);
        withEnv("KRB5_KDC", KRB5_KDC_EXTERNAL);
    }

    @Override
    public String getUsername() {
        return "system";
    }

    public String getKerberosUsername() {
        return "ORACLEUSR@" + KerberosContainer.KRB5_REALM;
    }

    @Override
    public OracleContainer withUsername(String username) {
        throw new UnsupportedOperationException("hardcoded setting, cannot change");
    }

    @Override
    public String getPassword() {
        return "oracle";
    }

    @Override
    public OracleContainer withPassword(String password) {
        throw new UnsupportedOperationException("hardcoded setting, cannot change");
    }

    @Override
    public String getDatabaseName() {
        return "FREE";
    }

    @Override
    public OracleContainer withDatabaseName(String dbName) {
        throw new UnsupportedOperationException("hardcoded setting, cannot change");
    }
}
