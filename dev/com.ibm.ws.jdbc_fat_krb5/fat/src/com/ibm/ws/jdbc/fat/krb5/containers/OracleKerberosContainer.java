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
public class OracleKerberosContainer extends OracleContainer implements KerberosAuthContainer {

    private static final Class<?> c = OracleKerberosContainer.class;

    /*
     * TODO: https://github.com/testcontainers/testcontainers-java/pull/10263
     * If this ever get's merged consider passing the future (RemoteDockerImage)
     * to the parent constructor so we can lazily start this image.
     */
    private static final DockerImageName ORACLE_KRB5 = ImageBuilder
                    .build("oracle-krb5:23.9-full-faststart")
                    .getDockerImageName()
                    .asCompatibleSubstituteFor("gvenzl/oracle-free");

    public static final String KRB5_USER = "ORACLEUSR";
    public static final String KRB5_PASS = "password";

    public OracleKerberosContainer(Network network) {
        super(ORACLE_KRB5);
        withNetwork(network);
        withNetworkAliases("oracle");
        withCreateContainerCmdModifier(cmd -> {
            cmd.withHostName("oracle");
        });
    }

    @Override
    protected void configure() {
        // Superclass settings
        super.withPassword("oracle");
        super.usingSid(); //Maintain current behavior of connecting with SID instead of pluggable database

        // Additional env variables
        withEnv("KRB5_REALM", KRB5_REALM);
        withEnv("KRB5_KDC", KRB5_KDC_EXTERNAL);

        // Wait strategy
        withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 3 : 25));

        // Logging
        withLogConsumer(new SimpleLogConsumer(c, "oracle-krb5"));

        // Default configuration
        super.configure();

    }

    @Override
    public String getSid() {
        return "FREE";
    }

    @Override
    public OracleContainer withUsername(String username) {
        throw new UnsupportedOperationException("hardcoded setting, cannot change");
    }

    @Override
    public OracleContainer withPassword(String password) {
        throw new UnsupportedOperationException("hardcoded setting, cannot change");
    }

    @Override
    public OracleContainer withDatabaseName(String dbName) {
        throw new UnsupportedOperationException("hardcoded setting, cannot change");
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
