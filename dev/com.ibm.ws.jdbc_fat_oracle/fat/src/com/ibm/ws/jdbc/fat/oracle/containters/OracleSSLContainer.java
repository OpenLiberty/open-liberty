/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.oracle.containters;

import java.time.Duration;

import org.testcontainers.utility.DockerImageName;

import com.ibm.ws.jdbc.fat.oracle.FATSuite;

import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.OracleXEContainer;

/**
 * Custom Oracle SSL Container class
 * TODO replace with OracleFree (org.testcontainers.oracle.OracleContainer)
 */
//public class OracleSSLContainer extends org.testcontainers.oracle.OracleContainer {
public class OracleSSLContainer extends OracleXEContainer {

    private static final int TCPS_PORT = 1522;
    private static final String WALLET_PASS = "WalletPasswd123";

    //TODO Start using ImageBuilder
//    private static final DockerImageName ORACLE_KRB5 = ImageBuilder
//                    .build("oracle-ssl:23-full-faststart")
//                    .getDockerImageName()
//                    .asCompatibleSubstituteFor("gvenzl/oracle-free");

    private static final String IMAGE_NAME_STRING = "kyleaure/oracle-21.3.0-faststart:1.0.full.ssl";
    private static final DockerImageName ORACLE_KRB5 = DockerImageName.parse(IMAGE_NAME_STRING).asCompatibleSubstituteFor("gvenzl/oracle-xe");

    public OracleSSLContainer() {
        super(ORACLE_KRB5);
        super.addExposedPort(TCPS_PORT);
        super.withPassword("oracle"); //Tell superclass the hardcoded password
        super.usingSid(); //Maintain current behavior of connecting with SID instead of pluggable database
        super.withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 3 : 25));
        super.withLogConsumer(new SimpleLogConsumer(FATSuite.class, "Oracle-SSL"));
    }

    //Do not allow developer to use a custom password
    @Override
//    public org.testcontainers.oracle.OracleContainer withPassword(String password) {
    public OracleXEContainer withPassword(String password) {
        throw new UnsupportedOperationException("Oracle SSL container does not support use of a customer password.");
    }

    public int getOracleSSLPort() {
        return this.getMappedPort(TCPS_PORT);
    }

    public String getJdbcSSLUrl() {
        return "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(HOST=" + getHost() + ")(PORT=" + getOracleSSLPort() + "))(CONNECT_DATA=(SERVICE_NAME=" + getSid() + ")))";
    }

    public String getWalletPassword() {
        return WALLET_PASS;
    }
}
