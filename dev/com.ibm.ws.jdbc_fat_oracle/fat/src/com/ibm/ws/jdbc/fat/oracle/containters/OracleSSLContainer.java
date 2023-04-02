/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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

import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import com.ibm.ws.jdbc.fat.oracle.FATSuite;

import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;

/**
 * Custom Oracle SSL Container class
 */
public class OracleSSLContainer extends OracleContainer {

    private static final int TCPS_PORT = 1522;
    private static final String WALLET_PASS = "WalletPasswd123";

    private static final String IMAGE_NAME_STRING = "kyleaure/oracle-21.3.0-faststart:1.0.full.ssl";
    private static final DockerImageName IMAGE_NAME = DockerImageName.parse(IMAGE_NAME_STRING).asCompatibleSubstituteFor("gvenzl/oracle-xe");

    public OracleSSLContainer() {
        super(IMAGE_NAME);
        super.addExposedPort(TCPS_PORT);
        super.withPassword("oracle"); //Tell superclass the hardcoded password
        super.usingSid(); //Maintain current behavior of connecting with SID instead of pluggable database
        super.withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 3 : 25));
        super.withLogConsumer(new SimpleLogConsumer(FATSuite.class, "Oracle-SSL"));
    }

    //Do not allow developer to use a custom password
    @Override
    public OracleContainer withPassword(String password) {
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
