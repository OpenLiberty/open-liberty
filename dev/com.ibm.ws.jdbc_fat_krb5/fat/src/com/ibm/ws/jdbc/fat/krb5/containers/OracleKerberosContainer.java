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

import static com.ibm.ws.jdbc.fat.krb5.containers.KerberosContainer.KRB5_KDC;
import static com.ibm.ws.jdbc.fat.krb5.containers.KerberosContainer.KRB5_REALM;

import java.time.Duration;

import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.OracleXEContainer;

/**
 * Custom Oracle Kerberos Container class
 * TODO replace with OracleFree (org.testcontainers.oracle.OracleContainer)
 */
//public class OracleKerberosContainer extends org.testcontainers.oracle.OracleContainer {
public class OracleKerberosContainer extends OracleXEContainer {

    private static final Class<?> c = OracleKerberosContainer.class;

    //TODO Start using ImageBuilder
//    private static final DockerImageName ORACLE_KRB5 = ImageBuilder
//                    .build("oracle-krb5:23.5-full-faststart")
//                    .getDockerImageName()
//                    .asCompatibleSubstituteFor("gvenzl/oracle-free");

    // NOTE: If this is ever updated, don't forget to push to docker hub, but DO NOT overwrite existing versions
    private static final String IMAGE_NAME_STRING = "kyleaure/oracle-21.3.0-faststart:1.0.full.krb5";
    private static final DockerImageName ORACLE_KRB5 = DockerImageName.parse(IMAGE_NAME_STRING).asCompatibleSubstituteFor("gvenzl/oracle-xe");

    public OracleKerberosContainer(Network network) {
        super(ORACLE_KRB5);
        super.withPassword("oracle"); //Tell superclass the hardcoded password
        super.usingSid(); //Maintain current behavior of connecting with SID instead of pluggable database
        super.withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 3 : 25));
        super.withNetwork(network);
        super.withLogConsumer(new SimpleLogConsumer(c, "oracle-krb5"));
    }

    @Override
    protected void configure() {
        withNetworkAliases("oracle");
        withCreateContainerCmdModifier(cmd -> {
            cmd.withHostName("oracle");
        });
        withEnv("KRB5_REALM", KRB5_REALM);
        withEnv("KRB5_KDC", KRB5_KDC);
        super.configure();
    }

    @Override
    public String getUsername() {
        return "system";
    }

    public String getKerberosUsername() {
        return "ORACLEUSR@" + KerberosContainer.KRB5_REALM;
    }

    @Override
    public OracleXEContainer withUsername(String username) {
        throw new UnsupportedOperationException("hardcoded setting, cannot change");
    }

    @Override
    public String getPassword() {
        return "oracle";
    }

    @Override
    public OracleXEContainer withPassword(String password) {
        throw new UnsupportedOperationException("hardcoded setting, cannot change");
    }

    @Override
    public String getDatabaseName() {
        return "XE";
    }

    @Override
    public OracleXEContainer withDatabaseName(String dbName) {
        throw new UnsupportedOperationException("hardcoded setting, cannot change");
    }
}
